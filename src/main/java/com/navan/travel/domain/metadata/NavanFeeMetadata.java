package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.domain.NavanFeeKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Navan-owned transaction charge metadata (first-class transaction)")
public record NavanFeeMetadata(
    @NotNull(message = "fee_kind is required for navan_fee")
    @JsonProperty("fee_kind")
    @Schema(description = "Category of Navan fee", example = "trip_fee")
    NavanFeeKind feeKind,

    @Schema(description = "Descriptive memo for the charge", example = "Navan trip fee", nullable = true)
    String description,

    @JsonProperty("related_external_id")
    @Schema(description = "External ID of the booking transaction this fee relates to", example = "flight-TXL-LHR-2026-03-11", nullable = true)
    String relatedExternalId
) implements TransactionMetadata {
}
