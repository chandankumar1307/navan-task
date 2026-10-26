package com.navan.travel.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.navan.travel.web.error.ErrorCode;

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
        throw new IllegalArgumentException(ErrorCode.UNKNOWN_NAVAN_FEE_KIND.formatMessage(value));
    }
}
