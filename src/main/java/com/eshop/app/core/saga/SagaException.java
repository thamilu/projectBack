package com.eshop.app.core.saga;

import com.eshop.app.core.exception.base.AppException;

public class SagaException extends AppException {
    public SagaException(String message) {
        super(message, "SAGA_EXECUTION_FAILED");
    }

    public SagaException(String message, Throwable cause) {
        super(message, "SAGA_EXECUTION_FAILED", cause);
    }
}
