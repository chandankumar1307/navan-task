package com.navan.travel.domain.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record HotelMetadata(
    @NotBlank(message = "name is required for hotel")
    String name,

    @NotNull(message = "address is required for hotel")
    @Valid
    HotelAddress address,

    @NotBlank(message = "check_in is required for hotel")
    @JsonProperty("check_in")
    String checkIn,

    @NotBlank(message = "check_out is required for hotel")
    @JsonProperty("check_out")
    String checkOut,

    @NotNull(message = "nights is required for hotel")
    @Min(value = 1, message = "nights must be at least 1")
    Integer nights,

    @NotNull(message = "room_rate_per_night is required for hotel")
    @JsonProperty("room_rate_per_night")
    BigDecimal roomRatePerNight,

    @Valid
    List<HotelExtra> extras
) implements TransactionMetadata {
}
