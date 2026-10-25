package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Hotel stay metadata")
public record HotelMetadata(
    @NotBlank(message = "name is required for hotel")
    @Schema(description = "Hotel property name", example = "Hotel Mitte London")
    String name,

    @NotNull(message = "address is required for hotel")
    @Valid
    @Schema(description = "Physical address of the hotel property")
    HotelAddress address,

    @NotBlank(message = "check_in is required for hotel")
    @JsonProperty("check_in")
    @Schema(description = "Check-in date (YYYY-MM-DD)", example = "2026-03-11")
    String checkIn,

    @NotBlank(message = "check_out is required for hotel")
    @JsonProperty("check_out")
    @Schema(description = "Check-out date (YYYY-MM-DD)", example = "2026-03-13")
    String checkOut,

    @NotNull(message = "nights is required for hotel")
    @Min(value = 1, message = "nights must be at least 1")
    @Schema(description = "Number of nights stayed", example = "2")
    Integer nights,

    @NotNull(message = "room_rate_per_night is required for hotel")
    @JsonProperty("room_rate_per_night")
    @Schema(description = "Room rate per night", example = "140.00")
    BigDecimal roomRatePerNight,

    @Valid
    @Schema(description = "Optional list of incidental stay extras")
    List<HotelExtra> extras
) implements TransactionMetadata {
}
