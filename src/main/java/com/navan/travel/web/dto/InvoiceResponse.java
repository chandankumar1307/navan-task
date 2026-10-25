package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.service.rollup.FeeRollupItem;
import com.navan.travel.service.rollup.TaxRollupItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Schema(description = "Invoice snapshot response representation matching expected invoice schema")
public record InvoiceResponse(
    @Schema(description = "Server-generated unique invoice identifier", example = "inv_80d79ceeb92e4abfae7813a824479532")
    String id,

    @Schema(description = "Invoice currency code shared across all included transactions", example = "EUR")
    String currency,

    @JsonProperty("grand_total")
    @Schema(description = "Sum of gross totals for all included transactions", example = "685.70")
    BigDecimal grandTotal,

    @Schema(description = "Categorized subtotals by modality and grand_total",
            example = "{\"flight\":220.00,\"hotel\":331.70,\"rail\":94.00,\"trip_fee\":25.00,\"agent_call_fee\":15.00,\"grand_total\":685.70}")
    Map<String, BigDecimal> totals,

    @JsonProperty("tax_rollup")
    @Schema(description = "Invoice-level tax rollup grouped by name and rate")
    List<TaxRollupItem> taxRollup,

    @JsonProperty("fee_rollup")
    @Schema(description = "Invoice-level fee rollup grouped by name")
    List<FeeRollupItem> feeRollup,

    @Schema(description = "List of frozen transaction snapshot items included on this invoice")
    List<InvoiceTransactionSnapshotDto> transactions,

    @JsonProperty("created_at")
    @Schema(description = "Timestamp when the invoice was generated (ISO-8601)", example = "2026-10-25T15:20:00Z")
    Instant createdAt,

    @JsonProperty("updated_at")
    @Schema(description = "Timestamp when the invoice was last modified (ISO-8601)", example = "2026-10-25T15:20:00Z")
    Instant updatedAt
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String currency;
        private BigDecimal grandTotal;
        private Map<String, BigDecimal> totals;
        private List<TaxRollupItem> taxRollup;
        private List<FeeRollupItem> feeRollup;
        private List<InvoiceTransactionSnapshotDto> transactions;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder grandTotal(BigDecimal grandTotal) {
            this.grandTotal = grandTotal;
            return this;
        }

        public Builder totals(Map<String, BigDecimal> totals) {
            this.totals = totals;
            return this;
        }

        public Builder taxRollup(List<TaxRollupItem> taxRollup) {
            this.taxRollup = taxRollup;
            return this;
        }

        public Builder feeRollup(List<FeeRollupItem> feeRollup) {
            this.feeRollup = feeRollup;
            return this;
        }

        public Builder transactions(List<InvoiceTransactionSnapshotDto> transactions) {
            this.transactions = transactions;
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

        public InvoiceResponse build() {
            return new InvoiceResponse(
                    id, currency, grandTotal, totals,
                    taxRollup, feeRollup, transactions,
                    createdAt, updatedAt
            );
        }
    }
}
