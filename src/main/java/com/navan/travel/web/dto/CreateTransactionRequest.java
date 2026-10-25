package com.navan.travel.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.navan.travel.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Request body for creating a multi-modal travel transaction")
public record CreateTransactionRequest(
    @NotNull(message = "type is required")
    @Schema(description = "Travel transaction modality type", example = "flight")
    TransactionType type,

    @NotBlank(message = "external_id is required")
    @JsonProperty("external_id")
    @Schema(description = "Client-supplied unique transaction identifier (e.g., booking/PNR reference)", example = "flight-TXL-LHR-2026-03-11")
    String externalId,

    @NotBlank(message = "currency is required")
    @Schema(description = "ISO-4217 three-letter currency code", example = "EUR")
    String currency,

    @NotNull(message = "occurred_at is required")
    @JsonProperty("occurred_at")
    @Schema(description = "Timestamp when the transaction occurred (ISO-8601)", example = "2026-03-11T06:40:00Z")
    Instant occurredAt,

    @NotNull(message = "total is required")
    @Schema(description = "Gross transaction total", example = "220.00")
    BigDecimal total,

    @NotNull(message = "tax_lines is required")
    @JsonProperty("tax_lines")
    @Valid
    @Schema(description = "List of structured tax lines (empty list allowed if no tax)")
    List<TaxLineDto> taxLines,

    @NotNull(message = "fee_lines is required")
    @JsonProperty("fee_lines")
    @Valid
    @Schema(description = "List of merchant/carrier fee lines (empty list allowed if no fees)")
    List<FeeLineDto> feeLines,

    @NotNull(message = "metadata is required")
    @Schema(description = "Type-specific metadata object (FlightMetadata, HotelMetadata, RailMetadata, or NavanFeeMetadata)",
            example = "{\"origin\":\"TXL\",\"destination\":\"LHR\",\"fare\":180.0,\"coupons\":[{\"flight_number\":\"BA982\",\"from\":\"TXL\",\"to\":\"LHR\",\"departure\":\"2026-03-11T07:00:00Z\",\"arrival\":\"2026-03-11T08:20:00Z\"}]}")
    JsonNode metadata
) {
}
