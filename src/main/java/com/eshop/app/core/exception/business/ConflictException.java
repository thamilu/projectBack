package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public class ConflictException extends BusinessException {
    public ConflictException(String message) {
        super(message, "CONFLICT", HttpStatus.CONFLICT);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, "CONFLICT", cause);
    }
}
