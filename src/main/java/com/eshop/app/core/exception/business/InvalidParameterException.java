package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public class InvalidParameterException extends BusinessException {
    public InvalidParameterException(String message) {
        super(message, "INVALID_PARAMETER", HttpStatus.BAD_REQUEST);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, "INVALID_PARAMETER", cause);
    }
}
