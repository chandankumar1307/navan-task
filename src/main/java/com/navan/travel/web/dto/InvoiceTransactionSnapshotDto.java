package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Deep copy snapshot of an included transaction frozen at invoice billing time")
public record InvoiceTransactionSnapshotDto(
    @JsonProperty("original_transaction_id")
    @Schema(description = "Identifier of the underlying transaction record", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
    String originalTransactionId,

    @Schema(description = "Transaction modality type", example = "flight")
    TransactionType type,

    @JsonProperty("occurred_at")
    @Schema(description = "Transaction occurrence timestamp (ISO-8601)", example = "2026-03-11T06:40:00Z")
    Instant occurredAt,

    @Schema(description = "Billed total snapshot amount", example = "220.00")
    BigDecimal total,

    @JsonProperty("tax_lines")
    @Schema(description = "Frozen tax lines on this transaction")
    List<TaxLineDto> taxLines,

    @JsonProperty("fee_lines")
    @Schema(description = "Frozen fee lines on this transaction")
    List<FeeLineDto> feeLines,

    @Schema(description = "Frozen type-specific metadata object")
    JsonNode metadata
) {
}
