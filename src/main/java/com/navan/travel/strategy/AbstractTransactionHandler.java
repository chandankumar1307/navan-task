package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.navan.travel.domain.metadata.TransactionMetadata;
import com.navan.travel.web.error.BadRequestException;
import com.navan.travel.web.error.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

public abstract class AbstractTransactionHandler<T extends TransactionMetadata> implements TransactionTypeHandler<T> {

    protected final ObjectMapper objectMapper;
    protected final Validator validator;

    protected AbstractTransactionHandler(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Override
    public T parseAndValidateMetadata(JsonNode metadataNode) {
        if (metadataNode == null || metadataNode.isNull() || metadataNode.isMissingNode() || metadataNode.isEmpty()) {
            throw new BadRequestException(ErrorCode.METADATA_REQUIRED, getType().getValue());
        }
        try {
            T metadata = objectMapper.treeToValue(metadataNode, getMetadataClass());
            if (metadata == null) {
                throw new BadRequestException(ErrorCode.METADATA_PARSE_FAILED, getType().getValue());
            }
            Set<ConstraintViolation<T>> violations = validator.validate(metadata);
            if (!violations.isEmpty()) {
                String errorMsg = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(ErrorCode.METADATA_VALIDATION_FAILED, getType().getValue(), errorMsg);
            }
            validateCustom(metadata);
            return metadata;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException(ErrorCode.METADATA_INVALID, getType().getValue(), e.getMessage());
        }
    }

    protected void validateCustom(T metadata) {
        // Subclass hook for domain-specific checks
    }

    @Override
    public String serializeMetadata(T metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize metadata for " + getType(), e);
        }
    }

    @Override
    public T deserializeMetadata(String metadataJson) {
        try {
            return objectMapper.readValue(metadataJson, getMetadataClass());
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize metadata for " + getType(), e);
        }
    }
}
