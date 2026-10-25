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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private TransactionType type;
        private String externalId;
        private String currency;
        private Instant occurredAt;
        private BigDecimal total;
        private List<TaxLineDto> taxLines;
        private List<FeeLineDto> feeLines;
        private JsonNode metadata;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(TransactionType type) {
            this.type = type;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder occurredAt(Instant occurredAt) {
            this.occurredAt = occurredAt;
            return this;
        }

        public Builder total(BigDecimal total) {
            this.total = total;
            return this;
        }

        public Builder taxLines(List<TaxLineDto> taxLines) {
            this.taxLines = taxLines;
            return this;
        }

        public Builder feeLines(List<FeeLineDto> feeLines) {
            this.feeLines = feeLines;
            return this;
        }

        public Builder metadata(JsonNode metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public TransactionResponse build() {
            return new TransactionResponse(
                    id, type, externalId, currency, occurredAt,
                    total, taxLines, feeLines, metadata, createdAt, updatedAt
            );
        }
    }
}
