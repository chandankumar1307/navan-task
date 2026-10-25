package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Structured tax line item (e.g., VAT, airport passenger taxes, governmental levies)")
public record TaxLineDto(
    @NotBlank(message = "tax name is required")
    @Schema(description = "Official tax or levy name", example = "VAT")
    String name,

    @Schema(description = "Optional tax percentage rate expressed as a decimal (e.g. 0.07 for 7%)", example = "0.07", nullable = true)
    BigDecimal rate,

    @NotNull(message = "tax amount is required")
    @Schema(description = "Tax monetary amount scaled to 2 decimal places", example = "21.70")
    BigDecimal amount
) {
}
