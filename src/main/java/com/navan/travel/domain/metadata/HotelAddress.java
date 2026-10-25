package com.navan.travel.domain.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Physical address for a hotel property")
public record HotelAddress(
    @NotBlank(message = "street is required for hotel address")
    @Schema(description = "Street address line", example = "12 Kingsway")
    String street,

    @NotBlank(message = "city is required for hotel address")
    @Schema(description = "City name", example = "London")
    String city,

    @NotBlank(message = "country is required for hotel address")
    @Schema(description = "Country code or name", example = "GB")
    String country
) {
}
