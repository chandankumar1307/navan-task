package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record RailLeg(
    @NotBlank(message = "origin is required for rail leg")
    String origin,

    @NotBlank(message = "destination is required for rail leg")
    String destination,

    @NotBlank(message = "departure is required for rail leg")
    String departure,

    @NotBlank(message = "arrival is required for rail leg")
    String arrival,

    @JsonProperty("train_number")
    String trainNumber,

    @JsonProperty("class")
    String railClass
) {
}
