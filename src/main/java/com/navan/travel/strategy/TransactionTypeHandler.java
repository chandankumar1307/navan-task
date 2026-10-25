package com.navan.travel.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;
import com.navan.travel.domain.entity.TransactionEntity;
import com.navan.travel.domain.metadata.TransactionMetadata;

public interface TransactionTypeHandler<T extends TransactionMetadata> {

    TransactionType getType();

    Class<T> getMetadataClass();

    T parseAndValidateMetadata(JsonNode metadataNode);

    String serializeMetadata(T metadata);

    T deserializeMetadata(String metadataJson);

    String getSubtotalCategory(TransactionEntity transaction, T metadata);
}
