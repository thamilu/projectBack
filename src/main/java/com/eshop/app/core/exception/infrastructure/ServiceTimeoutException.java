package com.eshop.app.core.exception.infrastructure;

import com.eshop.app.core.exception.base.InfrastructureException;

public class ServiceTimeoutException extends InfrastructureException {
    public ServiceTimeoutException(String message) {
        super(message, "SERVICE_TIMEOUT");
    }
    
    public ServiceTimeoutException(String message, Throwable cause) {
        super(message, "SERVICE_TIMEOUT", cause);
    }

    @Override
    public String getErrorCode() {
        return "SERVICE_TIMEOUT";
    }
}
