package com.navan.travel.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NavanFeeKind {
    TRIP_FEE("trip_fee"),
    AGENT_CALL_FEE("agent_call_fee");

    private final String value;

    NavanFeeKind(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static NavanFeeKind fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (NavanFeeKind kind : values()) {
            if (kind.value.equalsIgnoreCase(value.trim())) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown navan_fee fee_kind: " + value);
    }
}
