package com.navan.travel.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NavanFeeKindTest {

    @ParameterizedTest
    @CsvSource({
            "trip_fee, TRIP_FEE",
            "TRIP_FEE, TRIP_FEE",
            "agent_call_fee, AGENT_CALL_FEE",
            "AGENT_CALL_FEE, AGENT_CALL_FEE"
    })
    @DisplayName("fromValue parses case-insensitively and returns correct enum")
    void testFromValueValid(String input, NavanFeeKind expected) {
        assertThat(NavanFeeKind.fromValue(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("fromValue returns null when input is null")
    void testFromValueNull() {
        assertThat(NavanFeeKind.fromValue(null)).isNull();
    }

    @Test
    @DisplayName("fromValue throws IllegalArgumentException on unknown fee_kind")
    void testFromValueUnknown() {
        assertThatThrownBy(() -> NavanFeeKind.fromValue("mystery_fee"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown navan_fee fee_kind: mystery_fee");
    }

    @Test
    @DisplayName("getValue returns exact expected lowercase value")
    void testGetValue() {
        assertThat(NavanFeeKind.TRIP_FEE.getValue()).isEqualTo("trip_fee");
        assertThat(NavanFeeKind.AGENT_CALL_FEE.getValue()).isEqualTo("agent_call_fee");
    }
}
