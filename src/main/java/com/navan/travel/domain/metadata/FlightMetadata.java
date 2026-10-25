package com.navan.travel.domain.metadata;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record FlightMetadata(
    @NotBlank(message = "origin is required for flight")
    String origin,

    @NotBlank(message = "destination is required for flight")
    String destination,

    @NotNull(message = "fare is required for flight")
    BigDecimal fare,

    @NotEmpty(message = "coupons list cannot be empty for flight")
    @Valid
    List<FlightCoupon> coupons
) implements TransactionMetadata {
}
