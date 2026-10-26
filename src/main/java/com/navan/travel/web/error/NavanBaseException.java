package com.navan.travel.web.error;

/**
 * Base abstract exception for domain exceptions driven by {@link ErrorCode}.
 * Resolves the final exception message dynamically from the ErrorCode enum and optional arguments.
 */
public abstract class NavanBaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    protected NavanBaseException(ErrorCode errorCode, Object... args) {
        super(errorCode != null ? errorCode.formatMessage(args) : null);
        this.errorCode = errorCode;
        this.args = args != null ? args.clone() : new Object[0];
    }

    protected NavanBaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode != null ? errorCode.formatMessage(args) : null, cause);
        this.errorCode = errorCode;
        this.args = args != null ? args.clone() : new Object[0];
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args != null ? args.clone() : new Object[0];
    }
}
