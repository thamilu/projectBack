package com.eshop.app.user.domain.exception;

import com.eshop.app.core.exception.base.InfrastructureException;

public class CryptographicException extends InfrastructureException {
    public CryptographicException(String message, Throwable cause) {
        super(message, "CRYPTOGRAPHIC_ERROR", cause);
    }
}
