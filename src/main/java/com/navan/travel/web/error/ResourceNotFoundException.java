package com.navan.travel.web.error;

public class ResourceNotFoundException extends NavanBaseException {

    public ResourceNotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
    }

    public ResourceNotFoundException(String customMessage) {
        super(ErrorCode.RESOURCE_NOT_FOUND, customMessage);
    }
}
