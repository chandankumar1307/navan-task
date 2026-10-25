package com.navan.travel.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionTypeTest {

    @ParameterizedTest
    @CsvSource({
            "flight, FLIGHT",
            "FLIGHT, FLIGHT",
            "Flight, FLIGHT",
            "hotel, HOTEL",
            "HOTEL, HOTEL",
            "rail, RAIL",
            "navan_fee, NAVAN_FEE",
            "NAVAN_FEE, NAVAN_FEE"
    })
    @DisplayName("fromValue parses case-insensitively and returns correct enum")
    void testFromValueValid(String input, TransactionType expected) {
        assertThat(TransactionType.fromValue(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("fromValue returns null when input is null")
    void testFromValueNull() {
        assertThat(TransactionType.fromValue(null)).isNull();
    }

    @Test
    @DisplayName("fromValue throws IllegalArgumentException on unknown transaction type")
    void testFromValueUnknown() {
        assertThatThrownBy(() -> TransactionType.fromValue("cruise_ship"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown transaction type: cruise_ship");
    }

    @Test
    @DisplayName("getValue returns exact expected lowercase value")
    void testGetValue() {
        assertThat(TransactionType.FLIGHT.getValue()).isEqualTo("flight");
        assertThat(TransactionType.HOTEL.getValue()).isEqualTo("hotel");
        assertThat(TransactionType.RAIL.getValue()).isEqualTo("rail");
        assertThat(TransactionType.NAVAN_FEE.getValue()).isEqualTo("navan_fee");
    }
}
