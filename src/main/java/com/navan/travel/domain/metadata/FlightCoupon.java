package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Flight coupon / segment details")
public record FlightCoupon(
    @Schema(description = "Operating airline carrier code", example = "BA", nullable = true)
    String carrier,

    @NotBlank(message = "flight_number is required on each coupon")
    @JsonProperty("flight_number")
    @Schema(description = "Flight number", example = "BA982")
    String flightNumber,

    @NotBlank(message = "from is required on each coupon")
    @Schema(description = "Departure airport IATA code or city", example = "TXL")
    String from,

    @NotBlank(message = "to is required on each coupon")
    @Schema(description = "Arrival airport IATA code or city", example = "LHR")
    String to,

    @NotBlank(message = "departure is required on each coupon")
    @Schema(description = "Scheduled departure timestamp (ISO-8601)", example = "2026-03-11T07:00:00Z")
    String departure,

    @NotBlank(message = "arrival is required on each coupon")
    @Schema(description = "Scheduled arrival timestamp (ISO-8601)", example = "2026-03-11T08:20:00Z")
    String arrival
) {
}
