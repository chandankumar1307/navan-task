package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.navan.travel.domain.NavanFeeKind;
import jakarta.validation.constraints.NotNull;

public record NavanFeeMetadata(
    @NotNull(message = "fee_kind is required for navan_fee")
    @JsonProperty("fee_kind")
    NavanFeeKind feeKind,

    String description,

    @JsonProperty("related_external_id")
    String relatedExternalId
) implements TransactionMetadata {
}
