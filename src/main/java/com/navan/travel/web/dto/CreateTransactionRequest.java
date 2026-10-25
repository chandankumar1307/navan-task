package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CreateTransactionRequest(
    @NotNull(message = "type is required")
    TransactionType type,

    @NotBlank(message = "external_id is required")
    @JsonProperty("external_id")
    String externalId,

    @NotBlank(message = "currency is required")
    String currency,

    @NotNull(message = "occurred_at is required")
    @JsonProperty("occurred_at")
    Instant occurredAt,

    @NotNull(message = "total is required")
    BigDecimal total,

    @NotNull(message = "tax_lines is required")
    @JsonProperty("tax_lines")
    @Valid
    List<TaxLineDto> taxLines,

    @NotNull(message = "fee_lines is required")
    @JsonProperty("fee_lines")
    @Valid
    List<FeeLineDto> feeLines,

    @NotNull(message = "metadata is required")
    JsonNode metadata
) {
}
