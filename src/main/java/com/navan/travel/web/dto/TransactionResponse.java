package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Stored transaction record representation")
public record TransactionResponse(
    @Schema(description = "Server-generated unique identifier", example = "txn_6c97a7d42cf34d98947ab59bb4dfdb19")
    String id,

    @Schema(description = "Transaction modality type", example = "flight")
    TransactionType type,

    @JsonProperty("external_id")
    @Schema(description = "Client-supplied unique transaction identifier", example = "flight-TXL-LHR-2026-03-11")
    String externalId,

    @Schema(description = "ISO-4217 three-letter currency code", example = "EUR")
    String currency,

    @JsonProperty("occurred_at")
    @Schema(description = "Timestamp when the transaction occurred (ISO-8601)", example = "2026-03-11T06:40:00Z")
    Instant occurredAt,

    @Schema(description = "Gross transaction total", example = "220.00")
    BigDecimal total,

    @JsonProperty("tax_lines")
    @Schema(description = "Itemized tax lines")
    List<TaxLineDto> taxLines,

    @JsonProperty("fee_lines")
    @Schema(description = "Itemized carrier/merchant fee lines")
    List<FeeLineDto> feeLines,

    @Schema(description = "Type-specific metadata object")
    JsonNode metadata,

    @JsonProperty("created_at")
    @Schema(description = "Timestamp when the record was persisted in the service", example = "2026-10-25T15:20:00Z")
    Instant createdAt,

    @JsonProperty("updated_at")
    @Schema(description = "Timestamp when the record was last modified", example = "2026-10-25T15:20:00Z")
    Instant updatedAt
) {
}
