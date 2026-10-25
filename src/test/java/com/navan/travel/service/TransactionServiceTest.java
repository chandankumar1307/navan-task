package com.navan.travel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.FlightCoupon;
import com.navan.travel.domain.metadata.FlightMetadata;
import com.navan.travel.domain.repository.InvoiceTransactionSnapshotRepository;
import com.navan.travel.domain.repository.TransactionRepository;
import com.navan.travel.strategy.*;
import com.navan.travel.web.dto.*;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;
import com.navan.travel.web.error.ResourceNotFoundException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private InvoiceTransactionSnapshotRepository invoiceSnapshotRepository;

    private ObjectMapper objectMapper;
    private TransactionService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        TransactionHandlerRegistry registry = new TransactionHandlerRegistry(List.of(
                new FlightTransactionHandler(objectMapper, validator),
                new HotelTransactionHandler(objectMapper, validator),
                new RailTransactionHandler(objectMapper, validator),
                new NavanFeeTransactionHandler(objectMapper, validator)
        ));

        service = new TransactionService(transactionRepository, invoiceSnapshotRepository, registry, objectMapper);
    }

    @Test
    @DisplayName("createTransaction validates required fields")
    void testCreateRequiredFields() {
        CreateTransactionRequest reqNoType = new CreateTransactionRequest(null, "ext-1", "EUR", Instant.now(), new BigDecimal("100"), List.of(), List.of(), objectMapper.createObjectNode());
        assertThatThrownBy(() -> service.createTransaction(reqNoType))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("type is required");

        CreateTransactionRequest reqNoExtId = new CreateTransactionRequest(TransactionType.FLIGHT, "   ", "EUR", Instant.now(), new BigDecimal("100"), List.of(), List.of(), objectMapper.createObjectNode());
        assertThatThrownBy(() -> service.createTransaction(reqNoExtId))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("external_id is required");
    }

    @Test
    @DisplayName("createTransaction fast-path detects duplicate external_id and throws ConflictException with existing id")
    void testCreateDuplicateFastPath() {
        TransactionEntity existing = new TransactionEntity("txn_existing_123", "ext-duplicate", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        when(transactionRepository.findByExternalId("ext-duplicate")).thenReturn(Optional.of(existing));

        CreateTransactionRequest req = new CreateTransactionRequest(
                TransactionType.FLIGHT, "ext-duplicate", "EUR", Instant.now(), new BigDecimal("100.00"), List.of(), List.of(), objectMapper.createObjectNode()
        );

        assertThatThrownBy(() -> service.createTransaction(req))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> {
                    ConflictException ce = (ConflictException) ex;
                    assertThat(ce.getExistingId()).isEqualTo("txn_existing_123");
                });

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createTransaction successfully scales money and saves transaction with tax and fee lines")
    void testCreateSuccess() {
        when(transactionRepository.findByExternalId("ext-new")).thenReturn(Optional.empty());
        when(transactionRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        FlightCoupon coupon = new FlightCoupon("BA", "BA1", "TXL", "LHR", "2026-01-01T00:00:00Z", "2026-01-01T01:00:00Z");
        FlightMetadata metadata = new FlightMetadata("TXL", "LHR", new BigDecimal("100.00"), List.of(coupon));

        CreateTransactionRequest req = new CreateTransactionRequest(
                TransactionType.FLIGHT,
                "ext-new",
                "eur",
                Instant.parse("2026-03-11T10:00:00Z"),
                new BigDecimal("150.555"), // Should round to 150.56
                List.of(new TaxLineDto("VAT", new BigDecimal("0.0700"), new BigDecimal("10.555"))), // Should round to 10.56, rate 0.07
                List.of(new FeeLineDto("YQ", new BigDecimal("5.111"))), // Should round to 5.11
                objectMapper.valueToTree(metadata)
        );

        TransactionResponse res = service.createTransaction(req);

        assertThat(res.id()).startsWith("txn_");
        assertThat(res.currency()).isEqualTo("EUR");
        assertThat(res.total()).isEqualByComparingTo("150.56");
        assertThat(res.taxLines()).hasSize(1);
        assertThat(res.taxLines().get(0).amount()).isEqualByComparingTo("10.56");
        assertThat(res.taxLines().get(0).rate()).isEqualByComparingTo("0.07");
        assertThat(res.feeLines()).hasSize(1);
        assertThat(res.feeLines().get(0).amount()).isEqualByComparingTo("5.11");
    }

    @Test
    @DisplayName("getTransaction throws ResourceNotFoundException on non-existent id")
    void testGetNotFound() {
        when(transactionRepository.findById("missing_id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction("missing_id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found: missing_id");
    }

    @Test
    @DisplayName("listTransactions bounds limit > 100 with BadRequestException and defaults 0 or negative limits")
    void testListTransactionsBounds() {
        assertThatThrownBy(() -> service.listTransactions(101, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("limit cannot exceed 100");

        when(transactionRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // limit < 1 defaults to 50
        PageResponse<TransactionResponse> resDefault = service.listTransactions(0, -10);
        assertThat(resDefault.limit()).isEqualTo(50);
        assertThat(resDefault.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("patchTransaction blocks update if transaction is on an active invoice (409 Conflict)")
    void testPatchInvoicedTransactionBlocked() {
        TransactionEntity txn = new TransactionEntity("txn_1", "ext-1", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        when(transactionRepository.findById("txn_1")).thenReturn(Optional.of(txn));
        when(invoiceSnapshotRepository.existsByOriginalTransactionId("txn_1")).thenReturn(true);

        PatchTransactionRequest patchReq = new PatchTransactionRequest(new BigDecimal("200.00"), null, null, null, null);

        assertThatThrownBy(() -> service.patchTransaction("txn_1", patchReq))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Transaction is included on an active invoice and is frozen");

        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deleteTransaction blocks deletion if transaction is on an active invoice (409 Conflict)")
    void testDeleteInvoicedTransactionBlocked() {
        TransactionEntity txn = new TransactionEntity("txn_1", "ext-1", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        when(transactionRepository.findById("txn_1")).thenReturn(Optional.of(txn));
        when(invoiceSnapshotRepository.existsByOriginalTransactionId("txn_1")).thenReturn(true);

        assertThatThrownBy(() -> service.deleteTransaction("txn_1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Transaction is included on an active invoice and cannot be deleted");

        verify(transactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTransaction deletes entity if not invoiced")
    void testDeleteNonInvoicedTransactionSuccess() {
        TransactionEntity txn = new TransactionEntity("txn_free", "ext-free", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        when(transactionRepository.findById("txn_free")).thenReturn(Optional.of(txn));
        when(invoiceSnapshotRepository.existsByOriginalTransactionId("txn_free")).thenReturn(false);

        service.deleteTransaction("txn_free");

        verify(transactionRepository).delete(txn);
    }
}
