package com.navan.travel.domain.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RailMetadata(
    @NotEmpty(message = "legs list cannot be empty for rail")
    @Valid
    List<RailLeg> legs
) implements TransactionMetadata {
}
