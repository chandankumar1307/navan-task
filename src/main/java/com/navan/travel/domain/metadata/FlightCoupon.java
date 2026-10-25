package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record FlightCoupon(
    String carrier,

    @NotBlank(message = "flight_number is required on each coupon")
    @JsonProperty("flight_number")
    String flightNumber,

    @NotBlank(message = "from is required on each coupon")
    String from,

    @NotBlank(message = "to is required on each coupon")
    String to,

    @NotBlank(message = "departure is required on each coupon")
    String departure,

    @NotBlank(message = "arrival is required on each coupon")
    String arrival
) {
}
