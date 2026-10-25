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
}
