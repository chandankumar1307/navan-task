package com.navan.travel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.InvoiceEntity;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.repository.InvoiceRepository;
import com.navan.travel.domain.repository.TransactionRepository;
import com.navan.travel.service.rollup.FeeRollupAggregator;
import com.navan.travel.service.rollup.TaxRollupAggregator;
import com.navan.travel.strategy.*;
import com.navan.travel.web.dto.CreateInvoiceRequest;
import com.navan.travel.web.dto.InvoiceResponse;
import com.navan.travel.web.dto.PageResponse;
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
class InvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        TransactionHandlerRegistry registry = new TransactionHandlerRegistry(List.of(
                new FlightTransactionHandler(objectMapper, validator),
                new HotelTransactionHandler(objectMapper, validator),
                new RailTransactionHandler(objectMapper, validator),
                new NavanFeeTransactionHandler(objectMapper, validator)
        ));

        InvoiceSnapshotEngine snapshotEngine = new InvoiceSnapshotEngine();
        TaxRollupAggregator taxRollupAggregator = new TaxRollupAggregator();
        FeeRollupAggregator feeRollupAggregator = new FeeRollupAggregator();

        service = new InvoiceService(
                invoiceRepository,
                transactionRepository,
                snapshotEngine,
                taxRollupAggregator,
                feeRollupAggregator,
                registry,
                objectMapper
        );
    }

    @Test
    @DisplayName("createInvoice throws ResourceNotFoundException if any transaction ID is not found")
    void testCreateInvoiceTransactionNotFound() {
        when(transactionRepository.findById("txn_missing")).thenReturn(Optional.empty());

        CreateInvoiceRequest req = new CreateInvoiceRequest("txn_missing", null);

        assertThatThrownBy(() -> service.createInvoice(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Transaction not found for invoice: txn_missing");
    }

    @Test
    @DisplayName("createInvoice throws ConflictException (409) if transactions have mixed currencies")
    void testCreateInvoiceMixedCurrencies() {
        TransactionEntity eurTxn = new TransactionEntity("txn_1", "ext-1", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), "{}");
        TransactionEntity usdTxn = new TransactionEntity("txn_2", "ext-2", TransactionType.HOTEL, "USD", Instant.now(), new BigDecimal("200.00"), "{}");

        when(transactionRepository.findById("txn_1")).thenReturn(Optional.of(eurTxn));
        when(transactionRepository.findById("txn_2")).thenReturn(Optional.of(usdTxn));

        CreateInvoiceRequest req = new CreateInvoiceRequest(null, List.of("txn_1", "txn_2"));

        assertThatThrownBy(() -> service.createInvoice(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Mixed currencies detected");

        verify(invoiceRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("createInvoice successfully creates invoice and snapshots")
    void testCreateInvoiceSuccess() {
        String flightMeta = "{\"origin\":\"TXL\",\"destination\":\"LHR\",\"fare\":100.0,\"coupons\":[{\"flight_number\":\"BA1\",\"from\":\"TXL\",\"to\":\"LHR\",\"departure\":\"2026-01-01T00:00:00Z\",\"arrival\":\"2026-01-01T01:00:00Z\"}]}";
        String hotelMeta = "{\"name\":\"Hotel A\",\"address\":{\"street\":\"St 1\",\"city\":\"London\",\"country\":\"GB\"},\"check_in\":\"2026-01-01\",\"check_out\":\"2026-01-02\",\"nights\":1,\"room_rate_per_night\":200.0}";

        TransactionEntity txn1 = new TransactionEntity("txn_1", "ext-1", TransactionType.FLIGHT, "EUR", Instant.now(), new BigDecimal("100.00"), flightMeta);
        TransactionEntity txn2 = new TransactionEntity("txn_2", "ext-2", TransactionType.HOTEL, "EUR", Instant.now(), new BigDecimal("200.00"), hotelMeta);

        when(transactionRepository.findById("txn_1")).thenReturn(Optional.of(txn1));
        when(transactionRepository.findById("txn_2")).thenReturn(Optional.of(txn2));

        when(invoiceRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInvoiceRequest req = new CreateInvoiceRequest(null, List.of("txn_1", "txn_2"));
        InvoiceResponse res = service.createInvoice(req);

        assertThat(res.id()).startsWith("inv_");
        assertThat(res.currency()).isEqualTo("EUR");
        assertThat(res.grandTotal()).isEqualByComparingTo("300.00");
        assertThat(res.totals().get("flight")).isEqualByComparingTo("100.00");
        assertThat(res.totals().get("hotel")).isEqualByComparingTo("200.00");
        assertThat(res.totals().get("grand_total")).isEqualByComparingTo("300.00");
        assertThat(res.transactions()).hasSize(2);
    }

    @Test
    @DisplayName("getInvoice throws ResourceNotFoundException when invoice not found")
    void testGetInvoiceNotFound() {
        when(invoiceRepository.findById("missing_inv")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInvoice("missing_inv"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Invoice not found: missing_inv");
    }

    @Test
    @DisplayName("listInvoices rejects limit > 100 with BadRequestException and handles bounds")
    void testListInvoicesBounds() {
        assertThatThrownBy(() -> service.listInvoices(101, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("limit cannot exceed 100");

        when(invoiceRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<InvoiceResponse> res = service.listInvoices(0, -5);
        assertThat(res.limit()).isEqualTo(50);
        assertThat(res.offset()).isEqualTo(0);
    }

    @Test
    @DisplayName("deleteInvoice deletes invoice entity without altering original transactions")
    void testDeleteInvoiceSuccess() {
        InvoiceEntity invoice = new InvoiceEntity("inv_1", "EUR", new BigDecimal("100.00"));
        when(invoiceRepository.findByIdForUpdate("inv_1")).thenReturn(Optional.of(invoice));

        service.deleteInvoice("inv_1");

        verify(invoiceRepository).delete(invoice);
        // Note: transactionRepository is not touched during invoice deletion
        verifyNoInteractions(transactionRepository);
    }

    @Test
    @DisplayName("patchInvoice acquires pessimistic row lock and rebuilds snapshots cleanly")
    void testPatchInvoiceSuccess() {
        InvoiceEntity invoice = new InvoiceEntity("inv_1", "EUR", new BigDecimal("100.00"));
        TransactionEntity txn1 = new TransactionEntity("txn_1", "ext_1", TransactionType.FLIGHT, "EUR",
                Instant.parse("2026-03-11T06:40:00Z"), new BigDecimal("150.00"), "{}");

        when(invoiceRepository.findByIdForUpdate("inv_1")).thenReturn(Optional.of(invoice));
        when(transactionRepository.findById("txn_1")).thenReturn(Optional.of(txn1));
        when(invoiceRepository.saveAndFlush(any(InvoiceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateInvoiceRequest request = new CreateInvoiceRequest("txn_1", null);
        InvoiceResponse response = service.patchInvoice("inv_1", request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo("inv_1");
        assertThat(response.grandTotal()).isEqualByComparingTo("150.00");
        verify(invoiceRepository).findByIdForUpdate("inv_1");
    }
}
