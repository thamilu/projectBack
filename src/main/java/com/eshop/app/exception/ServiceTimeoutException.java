package com.eshop.app.exception;

public class ServiceTimeoutException extends RuntimeException {
    public ServiceTimeoutException(String message) {
        super(message);
    }
    
    public ServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    // Added for compatibility with GlobalExceptionHandler
    public String getErrorCode() {
        return "SERVICE_TIMEOUT";
    }
}
