package com.navan.travel.service.rollup;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxRollupItem(
    String name,
    BigDecimal rate,
    BigDecimal amount
) {
}
