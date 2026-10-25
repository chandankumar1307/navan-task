package com.navan.travel.domain.metadata;

import jakarta.validation.constraints.NotBlank;

public record HotelAddress(
    @NotBlank(message = "street is required for hotel address")
    String street,

    @NotBlank(message = "city is required for hotel address")
    String city,

    @NotBlank(message = "country is required for hotel address")
    String country
) {
}
