package com.navan.travel.web.error;

public class BadRequestException extends NavanBaseException {

    public BadRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public BadRequestException(String customMessage) {
        super(ErrorCode.BAD_REQUEST, customMessage);
    }
}
