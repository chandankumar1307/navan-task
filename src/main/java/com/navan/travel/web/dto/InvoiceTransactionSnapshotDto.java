package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record InvoiceTransactionSnapshotDto(
    @JsonProperty("original_transaction_id")
    String originalTransactionId,

    TransactionType type,

    @JsonProperty("occurred_at")
    Instant occurredAt,

    BigDecimal total,

    @JsonProperty("tax_lines")
    List<TaxLineDto> taxLines,

    @JsonProperty("fee_lines")
    List<FeeLineDto> feeLines,

    JsonNode metadata
) {
}
