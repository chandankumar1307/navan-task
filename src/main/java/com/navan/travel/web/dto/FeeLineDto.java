package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured fee line item (e.g., airline YQ fuel surcharges, booking fees)")
public record FeeLineDto(
    @NotBlank(message = "fee name is required")
    @Schema(description = "Commercial supplier fee name", example = "YQ")
    String name,

    @NotNull(message = "fee amount is required")
    @Schema(description = "Fee monetary amount scaled to 2 decimal places", example = "25.00")
    BigDecimal amount
) {
}
