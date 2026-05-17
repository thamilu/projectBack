package com.eshop.app.core.exception.base;

public class AppException extends RuntimeException {
    private final String errorCode;

    public AppException(String message) {
        super(message);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public AppException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INTERNAL_SERVER_ERROR";
    }

    public AppException(String message, String errorCode, Throwable cause) {
        super(message, errorCode != null ? cause : null);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
