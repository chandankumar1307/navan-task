package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Request body for patching mutable fields on a non-invoiced transaction")
public record PatchTransactionRequest(
    @Schema(description = "Updated gross total", example = "250.00", nullable = true)
    BigDecimal total,

    @JsonProperty("occurred_at")
    @Schema(description = "Updated occurrence timestamp (ISO-8601)", example = "2026-03-11T07:00:00Z", nullable = true)
    Instant occurredAt,

    @JsonProperty("tax_lines")
    @Valid
    @Schema(description = "Replacement list of tax lines", nullable = true)
    List<TaxLineDto> taxLines,

    @JsonProperty("fee_lines")
    @Valid
    @Schema(description = "Replacement list of fee lines", nullable = true)
    List<FeeLineDto> feeLines,

    @Schema(description = "Replacement type-specific metadata object", nullable = true)
    JsonNode metadata
) {
}
