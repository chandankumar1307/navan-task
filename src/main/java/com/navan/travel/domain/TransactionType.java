package com.navan.travel.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.navan.travel.web.error.ErrorCode;

public enum TransactionType {
    FLIGHT("flight"),
    HOTEL("hotel"),
    RAIL("rail"),
    NAVAN_FEE("navan_fee");

    private final String value;

    TransactionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TransactionType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (TransactionType type : values()) {
            if (type.value.equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        throw new IllegalArgumentException(ErrorCode.UNKNOWN_TRANSACTION_TYPE.formatMessage(value));
    }
}
