package com.navan.travel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.entity.InvoiceEntity;
import com.navan.travel.domain.entity.InvoiceFeeLineSnapshotEntity;
import com.navan.travel.domain.entity.InvoiceTaxLineSnapshotEntity;
import com.navan.travel.domain.entity.InvoiceTransactionSnapshotEntity;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.TransactionMetadata;
import com.navan.travel.domain.repository.InvoiceRepository;
import com.navan.travel.domain.repository.TransactionRepository;
import com.navan.travel.service.rollup.FeeRollupAggregator;
import com.navan.travel.service.rollup.FeeRollupItem;
import com.navan.travel.service.rollup.TaxRollupAggregator;
import com.navan.travel.service.rollup.TaxRollupItem;
import com.navan.travel.strategy.TransactionHandlerRegistry;
import com.navan.travel.strategy.TransactionTypeHandler;
import com.navan.travel.web.dto.*;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;
import com.navan.travel.web.error.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TransactionRepository transactionRepository;
    private final InvoiceSnapshotEngine snapshotEngine;
    private final TaxRollupAggregator taxRollupAggregator;
    private final FeeRollupAggregator feeRollupAggregator;
    private final TransactionHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper;

    public InvoiceService(InvoiceRepository invoiceRepository,
                          TransactionRepository transactionRepository,
                          InvoiceSnapshotEngine snapshotEngine,
                          TaxRollupAggregator taxRollupAggregator,
                          FeeRollupAggregator feeRollupAggregator,
                          TransactionHandlerRegistry handlerRegistry,
                          ObjectMapper objectMapper) {
        this.invoiceRepository = invoiceRepository;
        this.transactionRepository = transactionRepository;
        this.snapshotEngine = snapshotEngine;
        this.taxRollupAggregator = taxRollupAggregator;
        this.feeRollupAggregator = feeRollupAggregator;
        this.handlerRegistry = handlerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        List<String> txnIds = request.resolveTransactionIds();
        List<TransactionEntity> transactions = loadTransactions(txnIds);

        validateCurrencies(transactions);

        String currency = transactions.get(0).getCurrency();
        String invoiceId = "inv_" + UUID.randomUUID().toString().replace("-", "");
        BigDecimal grandTotal = calculateGrandTotal(transactions);

        InvoiceEntity invoice = new InvoiceEntity(invoiceId, currency, grandTotal);
        snapshotEngine.createSnapshots(transactions, invoice);

        InvoiceEntity saved = invoiceRepository.saveAndFlush(invoice);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        return toResponse(invoice);
    }

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> listInvoices(int limit, long offset) {
        if (limit > 100) {
            throw new BadRequestException("limit cannot exceed 100");
        }
        if (limit < 1) {
            limit = 50;
        }
        if (offset < 0) {
            offset = 0;
        }

        int pageNumber = (int) (offset / limit);
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Page<InvoiceEntity> page = invoiceRepository.findAll(pageable);

        List<InvoiceResponse> items = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(items, limit, offset, page.getTotalElements());
    }

    @Transactional
    public InvoiceResponse patchInvoice(String id, CreateInvoiceRequest request) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));

        List<String> txnIds = request.resolveTransactionIds();
        List<TransactionEntity> transactions = loadTransactions(txnIds);
        validateCurrencies(transactions);

        String currency = transactions.get(0).getCurrency();
        BigDecimal grandTotal = calculateGrandTotal(transactions);

        invoice.clearSnapshots();
        snapshotEngine.createSnapshots(transactions, invoice);
        invoice.setCurrency(currency);
        invoice.setGrandTotal(grandTotal);

        InvoiceEntity saved = invoiceRepository.saveAndFlush(invoice);
        return toResponse(saved);
    }

    @Transactional
    public void deleteInvoice(String id) {
        InvoiceEntity invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        invoiceRepository.delete(invoice);
    }

    private List<TransactionEntity> loadTransactions(List<String> txnIds) {
        List<TransactionEntity> transactions = new ArrayList<>();
        for (String txnId : txnIds) {
            TransactionEntity txn = transactionRepository.findById(txnId)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for invoice: " + txnId));
            transactions.add(txn);
        }
        return transactions;
    }

    private void validateCurrencies(List<TransactionEntity> transactions) {
        String baseCurrency = transactions.get(0).getCurrency();
        for (TransactionEntity txn : transactions) {
            if (!baseCurrency.equalsIgnoreCase(txn.getCurrency())) {
                throw new ConflictException("Mixed currencies detected: all transactions on an invoice must share the same currency (expected "
                        + baseCurrency + ", found " + txn.getCurrency() + ")");
            }
        }
    }

    private BigDecimal calculateGrandTotal(List<TransactionEntity> transactions) {
        BigDecimal sum = BigDecimal.ZERO;
        for (TransactionEntity txn : transactions) {
            sum = sum.add(txn.getTotal());
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    public InvoiceResponse toResponse(InvoiceEntity invoice) {
        List<InvoiceTaxLineSnapshotEntity> allTaxes = new ArrayList<>();
        List<InvoiceFeeLineSnapshotEntity> allFees = new ArrayList<>();
        List<InvoiceTransactionSnapshotDto> snapshotDtos = new ArrayList<>();

        Map<String, BigDecimal> totalsMap = new LinkedHashMap<>();
        BigDecimal calculatedGrandTotal = BigDecimal.ZERO;

        if (invoice.getSnapshots() != null) {
            for (InvoiceTransactionSnapshotEntity snapshot : invoice.getSnapshots()) {
                allTaxes.addAll(snapshot.getTaxLines());
                allFees.addAll(snapshot.getFeeLines());

                // Resolve subtotal category using Strategy handler
                TransactionTypeHandler<TransactionMetadata> handler = handlerRegistry.getHandler(snapshot.getTransactionType());
                TransactionMetadata meta = handler.deserializeMetadata(snapshot.getMetadataJson());
                String category = handler.getSubtotalCategory(null, meta);

                totalsMap.merge(category, snapshot.getTotal(), BigDecimal::add);
                calculatedGrandTotal = calculatedGrandTotal.add(snapshot.getTotal());

                JsonNode metaNode;
                try {
                    metaNode = objectMapper.readTree(snapshot.getMetadataJson());
                } catch (Exception e) {
                    metaNode = null;
                }

                List<TaxLineDto> taxLineDtos = snapshot.getTaxLines().stream()
                        .map(t -> new TaxLineDto(t.getName(), t.getRate(), t.getAmount()))
                        .toList();

                List<FeeLineDto> feeLineDtos = snapshot.getFeeLines().stream()
                        .map(f -> new FeeLineDto(f.getName(), f.getAmount()))
                        .toList();

                snapshotDtos.add(new InvoiceTransactionSnapshotDto(
                        snapshot.getOriginalTransactionId(),
                        snapshot.getTransactionType(),
                        snapshot.getOccurredAt(),
                        snapshot.getTotal(),
                        taxLineDtos,
                        feeLineDtos,
                        metaNode
                ));
            }
        }

        // Scale subtotals to 2 decimal places
        Map<String, BigDecimal> formattedTotals = new LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : totalsMap.entrySet()) {
            formattedTotals.put(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP));
        }
        formattedTotals.put("grand_total", calculatedGrandTotal.setScale(2, RoundingMode.HALF_UP));

        List<TaxRollupItem> taxRollup = taxRollupAggregator.aggregate(allTaxes);
        List<FeeRollupItem> feeRollup = feeRollupAggregator.aggregate(allFees);

        return InvoiceResponse.builder()
                .id(invoice.getId())
                .currency(invoice.getCurrency())
                .grandTotal(invoice.getGrandTotal().setScale(2, RoundingMode.HALF_UP))
                .totals(formattedTotals)
                .taxRollup(taxRollup)
                .feeRollup(feeRollup)
                .transactions(snapshotDtos)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }
}
