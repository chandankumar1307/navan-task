package com.navan.travel.web.error;

public class ConflictException extends NavanBaseException {

    private final String existingId;

    public ConflictException(ErrorCode errorCode, Object... args) {
        super(errorCode, args);
        this.existingId = null;
    }

    public static ConflictException withExistingId(ErrorCode errorCode, String existingId, Object... args) {
        return new ConflictException(existingId, errorCode, args);
    }

    private ConflictException(String existingId, ErrorCode errorCode, Object... args) {
        super(errorCode, args);
        this.existingId = existingId;
    }

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
        this.existingId = null;
    }

    public ConflictException(String message, String existingId) {
        super(ErrorCode.CONFLICT, message);
        this.existingId = existingId;
    }

    public String getExistingId() {
        return existingId;
    }
}
