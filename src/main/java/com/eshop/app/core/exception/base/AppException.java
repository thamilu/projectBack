package com.eshop.app.core.exception.base;

/**
 * Base runtime exception for all application-level exceptions.
 *
 * <p>Guarantees a non-null {@code errorCode}: if no error code is supplied (or
 * {@code null} is explicitly passed), it defaults to {@code "INTERNAL_SERVER_ERROR"}.
 * When a {@link Throwable} cause is supplied, it is always preserved and never
 * silently discarded, regardless of whether an {@code errorCode} was also supplied.
 */
public class AppException extends RuntimeException {

    private static final String DEFAULT_ERROR_CODE = "INTERNAL_SERVER_ERROR";

    private final String errorCode;

    public AppException(String message) {
        super(message);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public AppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
    }

    public AppException(Throwable cause) {
        super(cause != null ? cause.getMessage() : null, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public AppException(Throwable cause, String errorCode) {
        super(cause != null ? cause.getMessage() : null, cause);
        this.errorCode = errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = DEFAULT_ERROR_CODE;
    }

    public AppException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
    }

    protected AppException(
            String message,
            String errorCode,
            Throwable cause,
            boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.errorCode = errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
