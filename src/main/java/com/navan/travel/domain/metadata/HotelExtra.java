package com.navan.travel.domain.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Incidental hotel extra (e.g., breakfast, parking, wifi)")
public record HotelExtra(
    @NotBlank(message = "extra name is required")
    @Schema(description = "Extra amenity name", example = "Breakfast")
    String name,

    @NotNull(message = "extra amount is required")
    @Schema(description = "Monetary amount for the extra", example = "30.00")
    BigDecimal amount
) {
}
