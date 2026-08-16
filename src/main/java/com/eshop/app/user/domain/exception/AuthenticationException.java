package com.eshop.app.user.domain.exception;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public abstract class AuthenticationException extends BusinessException {
    protected AuthenticationException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
    }
}
