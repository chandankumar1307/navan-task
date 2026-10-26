package com.navan.travel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.entity.TransactionFeeLineEntity;
import com.navan.travel.domain.entity.TransactionTaxLineEntity;
import com.navan.travel.domain.metadata.TransactionMetadata;
import com.navan.travel.domain.repository.InvoiceTransactionSnapshotRepository;
import com.navan.travel.domain.repository.TransactionRepository;
import com.navan.travel.strategy.TransactionHandlerRegistry;
import com.navan.travel.strategy.TransactionTypeHandler;
import com.navan.travel.web.dto.*;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ConflictException;
import com.navan.travel.web.error.ErrorCode;
import com.navan.travel.web.error.ResourceNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final InvoiceTransactionSnapshotRepository invoiceSnapshotRepository;
    private final TransactionHandlerRegistry handlerRegistry;
    private final ObjectMapper objectMapper;

    public TransactionService(TransactionRepository transactionRepository,
                              InvoiceTransactionSnapshotRepository invoiceSnapshotRepository,
                              TransactionHandlerRegistry handlerRegistry,
                              ObjectMapper objectMapper) {
        this.transactionRepository = transactionRepository;
        this.invoiceSnapshotRepository = invoiceSnapshotRepository;
        this.handlerRegistry = handlerRegistry;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request) {
        if (request.type() == null) {
            throw new BadRequestException(ErrorCode.TRANSACTION_TYPE_REQUIRED);
        }
        if (request.externalId() == null || request.externalId().isBlank()) {
            throw new BadRequestException(ErrorCode.EXTERNAL_ID_REQUIRED);
        }

        // Fast-path duplicate check
        transactionRepository.findByExternalId(request.externalId().trim()).ifPresent(existing -> {
            throw ConflictException.withExistingId(ErrorCode.TRANSACTION_EXTERNAL_ID_CONFLICT, existing.getId(), request.externalId());
        });

        // Validate metadata using Strategy handler
        TransactionTypeHandler<TransactionMetadata> handler = handlerRegistry.getHandler(request.type());
        TransactionMetadata metadata = handler.parseAndValidateMetadata(request.metadata());
        String metadataJson = handler.serializeMetadata(metadata);

        String id = "txn_" + UUID.randomUUID().toString().replace("-", "");
        BigDecimal scaledTotal = request.total().setScale(2, RoundingMode.HALF_UP);

        TransactionEntity entity = new TransactionEntity(
                id,
                request.externalId().trim(),
                request.type(),
                request.currency().trim().toUpperCase(),
                request.occurredAt(),
                scaledTotal,
                metadataJson
        );

        if (request.taxLines() != null) {
            for (TaxLineDto tax : request.taxLines()) {
                BigDecimal scaledTaxAmount = tax.amount().setScale(2, RoundingMode.HALF_UP);
                BigDecimal normalizedRate = tax.rate() != null ? tax.rate().stripTrailingZeros() : null;
                entity.addTaxLine(new TransactionTaxLineEntity(tax.name(), normalizedRate, scaledTaxAmount));
            }
        }

        if (request.feeLines() != null) {
            for (FeeLineDto fee : request.feeLines()) {
                BigDecimal scaledFeeAmount = fee.amount().setScale(2, RoundingMode.HALF_UP);
                entity.addFeeLine(new TransactionFeeLineEntity(fee.name(), scaledFeeAmount));
            }
        }

        TransactionEntity saved = transactionRepository.saveAndFlush(entity);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(String id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> listTransactions(int limit, long offset) {
        if (limit > 100) {
            throw new BadRequestException(ErrorCode.LIMIT_EXCEEDED);
        }
        if (limit < 1) {
            limit = 50;
        }
        if (offset < 0) {
            offset = 0;
        }

        Pageable pageable = new OffsetLimitRequest(offset, limit, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        Slice<TransactionEntity> slice = transactionRepository.findSliceBy(pageable);

        List<TransactionResponse> items = slice.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(items, limit, offset, slice.hasNext());
    }

    @Transactional
    public TransactionResponse patchTransaction(String id, PatchTransactionRequest request) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, id));

        // Invariant: If on any invoice, return 409 and mutate nothing
        if (invoiceSnapshotRepository.existsByOriginalTransactionId(id)) {
            throw new ConflictException(ErrorCode.TRANSACTION_FROZEN_MUTATION_BLOCKED);
        }

        if (request.total() != null) {
            entity.setTotal(request.total().setScale(2, RoundingMode.HALF_UP));
        }
        if (request.occurredAt() != null) {
            entity.setOccurredAt(request.occurredAt());
        }
        if (request.taxLines() != null) {
            entity.clearTaxLines();
            for (TaxLineDto tax : request.taxLines()) {
                BigDecimal scaledTaxAmount = tax.amount().setScale(2, RoundingMode.HALF_UP);
                BigDecimal normalizedRate = tax.rate() != null ? tax.rate().stripTrailingZeros() : null;
                entity.addTaxLine(new TransactionTaxLineEntity(tax.name(), normalizedRate, scaledTaxAmount));
            }
        }
        if (request.feeLines() != null) {
            entity.clearFeeLines();
            for (FeeLineDto fee : request.feeLines()) {
                BigDecimal scaledFeeAmount = fee.amount().setScale(2, RoundingMode.HALF_UP);
                entity.addFeeLine(new TransactionFeeLineEntity(fee.name(), scaledFeeAmount));
            }
        }
        if (request.metadata() != null) {
            TransactionTypeHandler<TransactionMetadata> handler = handlerRegistry.getHandler(entity.getType());
            TransactionMetadata metadata = handler.parseAndValidateMetadata(request.metadata());
            entity.setMetadataJson(handler.serializeMetadata(metadata));
        }

        TransactionEntity saved = transactionRepository.saveAndFlush(entity);
        return toResponse(saved);
    }

    @Transactional
    public void deleteTransaction(String id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND, id));

        // Invariant: If on any invoice, return 409 and delete nothing
        if (invoiceSnapshotRepository.existsByOriginalTransactionId(id)) {
            throw new ConflictException(ErrorCode.TRANSACTION_FROZEN_DELETION_BLOCKED);
        }

        transactionRepository.delete(entity);
    }

    public TransactionResponse toResponse(TransactionEntity entity) {
        JsonNode metadataNode;
        try {
            metadataNode = objectMapper.readTree(entity.getMetadataJson());
        } catch (Exception e) {
            metadataNode = null;
        }

        List<TaxLineDto> taxLines = entity.getTaxLines() != null
                ? entity.getTaxLines().stream()
                .map(t -> new TaxLineDto(t.getName(), t.getRate(), t.getAmount()))
                .toList()
                : List.of();

        List<FeeLineDto> feeLines = entity.getFeeLines() != null
                ? entity.getFeeLines().stream()
                .map(f -> new FeeLineDto(f.getName(), f.getAmount()))
                .toList()
                : List.of();

        return new TransactionResponse(
                entity.getId(),
                entity.getType(),
                entity.getExternalId(),
                entity.getCurrency(),
                entity.getOccurredAt(),
                entity.getTotal(),
                taxLines,
                feeLines,
                metadataNode,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
