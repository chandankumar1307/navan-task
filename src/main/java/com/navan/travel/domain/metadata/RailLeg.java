package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Individual leg of a rail journey")
public record RailLeg(
    @NotBlank(message = "origin is required for rail leg")
    @Schema(description = "Leg origin station", example = "London St Pancras")
    String origin,

    @NotBlank(message = "destination is required for rail leg")
    @Schema(description = "Leg destination station", example = "Lille Europe")
    String destination,

    @NotBlank(message = "departure is required for rail leg")
    @Schema(description = "Scheduled departure timestamp (ISO-8601)", example = "2026-03-13T09:00:00Z")
    String departure,

    @NotBlank(message = "arrival is required for rail leg")
    @Schema(description = "Scheduled arrival timestamp (ISO-8601)", example = "2026-03-13T10:30:00Z")
    String arrival,

    @JsonProperty("train_number")
    @Schema(description = "Train or service number", example = "EST9108", nullable = true)
    String trainNumber,

    @JsonProperty("class")
    @Schema(description = "Travel class (e.g., standard, first)", example = "standard", nullable = true)
    String railClass
) {
}
