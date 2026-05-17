package com.eshop.app.catalog.shared.exception;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

public class FriendlyUrlGenerationException extends BusinessException {
    
    public FriendlyUrlGenerationException(String message) {
        super(message, "URL_GENERATION_FAILED", HttpStatus.CONFLICT);
    }
    
    public FriendlyUrlGenerationException(String message, Throwable cause) {
        super(message, "URL_GENERATION_FAILED", cause);
    }
}
