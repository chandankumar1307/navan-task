package com.navan.travel.domain.metadata;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record HotelExtra(
    @NotBlank(message = "extra name is required")
    String name,

    @NotNull(message = "extra amount is required")
    BigDecimal amount
) {
}
