package com.navan.travel.service.rollup;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Aggregated carrier/merchant fee rollup item grouped by name")
public record FeeRollupItem(
    @Schema(description = "Carrier or merchant fee name", example = "YQ")
    String name,

    @Schema(description = "Aggregated monetary amount scaled to 2 decimal places", example = "25.00")
    BigDecimal amount
) {
}
