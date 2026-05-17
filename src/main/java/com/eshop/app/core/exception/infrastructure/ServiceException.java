package com.eshop.app.core.exception.infrastructure;

import com.eshop.app.core.exception.base.InfrastructureException;

public class ServiceException extends InfrastructureException {
    public ServiceException(String message) {
        super(message, "SERVICE_ERROR");
    }
    
    public ServiceException(String message, Throwable cause) {
        super(message, "SERVICE_ERROR", cause);
    }
}
