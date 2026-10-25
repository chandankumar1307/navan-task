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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

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
        // Enforce Global Lock Ordering Hierarchy: Transactions FIRST (lexicographically sorted), Invoice SECOND
        List<String> txnIds = request.resolveTransactionIds();
        List<String> sortedTxnIds = txnIds.stream().sorted().toList();
        List<TransactionEntity> transactions = loadTransactions(sortedTxnIds);

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

        Pageable pageable = new OffsetLimitRequest(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Slice<InvoiceEntity> slice = invoiceRepository.findSliceBy(pageable);

        List<InvoiceResponse> items = slice.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(items, limit, offset, slice.hasNext());
    }

    @Transactional
    public InvoiceResponse patchInvoice(String id, CreateInvoiceRequest request) {
        // Enforce Global Lock Ordering Hierarchy: Transactions FIRST (lexicographically sorted), Invoice SECOND
        List<String> txnIds = request.resolveTransactionIds();
        List<String> sortedTxnIds = txnIds.stream().sorted().toList();
        List<TransactionEntity> transactions = loadTransactions(sortedTxnIds);
        validateCurrencies(transactions);

        InvoiceEntity invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));

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
        InvoiceEntity invoice = invoiceRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        invoiceRepository.delete(invoice);
    }

    private List<TransactionEntity> loadTransactions(List<String> txnIds) {
        List<TransactionEntity> transactions = transactionRepository.findAllById(txnIds);
        if (transactions.size() != txnIds.size()) {
            Set<String> foundIds = transactions.stream()
                    .map(TransactionEntity::getId)
                    .collect(Collectors.toSet());
            for (String id : txnIds) {
                if (!foundIds.contains(id)) {
                    throw new ResourceNotFoundException("Transaction not found for invoice: " + id);
                }
            }
        }
        // Preserve caller's input order
        Map<String, TransactionEntity> map = transactions.stream()
                .collect(Collectors.toMap(TransactionEntity::getId, t -> t));
        List<TransactionEntity> ordered = new ArrayList<>(txnIds.size());
        for (String id : txnIds) {
            ordered.add(map.get(id));
        }
        return ordered;
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
                String category = handler.getSubtotalCategory(meta);

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

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getCurrency(),
                invoice.getGrandTotal().setScale(2, RoundingMode.HALF_UP),
                formattedTotals,
                taxRollup,
                feeRollup,
                snapshotDtos,
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }
}
