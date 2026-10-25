package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record TransactionResponse(
    String id,
    TransactionType type,
    @JsonProperty("external_id") String externalId,
    String currency,
    @JsonProperty("occurred_at") Instant occurredAt,
    BigDecimal total,
    @JsonProperty("tax_lines") List<TaxLineDto> taxLines,
    @JsonProperty("fee_lines") List<FeeLineDto> feeLines,
    JsonNode metadata,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt
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
            return new TransactionResponse(id, type, externalId, currency, occurredAt, total,
                    taxLines, feeLines, metadata, createdAt, updatedAt);
        }
    }
}
