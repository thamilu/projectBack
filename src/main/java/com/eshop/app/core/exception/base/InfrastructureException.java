package com.eshop.app.core.exception.base;

public class InfrastructureException extends AppException {
    private static final long serialVersionUID = 1L;

    public InfrastructureException(String message) {
        super(message, "INFRASTRUCTURE_ERROR");
    }

    public InfrastructureException(String message, String errorCode) {
        super(message, errorCode);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, "INFRASTRUCTURE_ERROR", cause);
    }

    public InfrastructureException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }
}
