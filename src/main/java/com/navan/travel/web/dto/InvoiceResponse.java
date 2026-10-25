package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.service.rollup.FeeRollupItem;
import com.navan.travel.service.rollup.TaxRollupItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record InvoiceResponse(
    String id,
    String currency,
    @JsonProperty("grand_total") BigDecimal grandTotal,
    Map<String, BigDecimal> totals,
    @JsonProperty("tax_rollup") List<TaxRollupItem> taxRollup,
    @JsonProperty("fee_rollup") List<FeeRollupItem> feeRollup,
    List<InvoiceTransactionSnapshotDto> transactions,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt
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
            return new InvoiceResponse(id, currency, grandTotal, totals, taxRollup, feeRollup, transactions, createdAt, updatedAt);
        }
    }
}
