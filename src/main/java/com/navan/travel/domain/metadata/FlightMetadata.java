package com.navan.travel.domain.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Flight-specific breakdown metadata")
public record FlightMetadata(
    @NotBlank(message = "origin is required for flight")
    @Schema(description = "Journey origin IATA or city", example = "TXL")
    String origin,

    @NotBlank(message = "destination is required for flight")
    @Schema(description = "Journey destination IATA or city", example = "LHR")
    String destination,

    @NotNull(message = "fare is required for flight")
    @Schema(description = "Base airfare component", example = "180.00")
    BigDecimal fare,

    @NotEmpty(message = "coupons list cannot be empty for flight")
    @Valid
    @Schema(description = "List of coupon flight segments (multi-coupon for round-trips)")
    List<FlightCoupon> coupons
) implements TransactionMetadata {
}
