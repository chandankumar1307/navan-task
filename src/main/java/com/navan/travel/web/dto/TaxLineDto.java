package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaxLineDto(
    @NotBlank(message = "tax name is required")
    String name,

    BigDecimal rate,

    @NotNull(message = "tax amount is required")
    BigDecimal amount
) {
}
