package com.navan.travel.service.rollup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated tax rollup item grouped by name and percentage rate")
public record TaxRollupItem(
    @Schema(description = "Tax or levy name", example = "VAT")
    String name,

    @Schema(description = "Tax percentage rate expressed as a decimal", example = "0.07", nullable = true)
    BigDecimal rate,

    @Schema(description = "Aggregated monetary amount scaled to 2 decimal places", example = "21.70")
    BigDecimal amount
) {
}
