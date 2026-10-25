package com.navan.travel.domain.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Rail journey metadata containing multi-segment legs")
public record RailMetadata(
    @NotEmpty(message = "legs list cannot be empty for rail")
    @Valid
    @Schema(description = "Sequence of journey legs (two or more legs for through journeys with train changes)")
    List<RailLeg> legs
) implements TransactionMetadata {
}
