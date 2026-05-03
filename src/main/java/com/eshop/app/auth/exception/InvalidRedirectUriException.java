package com.eshop.app.auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when redirect URI validation fails.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRedirectUriException extends RuntimeException {
    
    public InvalidRedirectUriException(String message) {
        super(message);
    }
    
    public InvalidRedirectUriException(String message, Throwable cause) {
        super(message, cause);
    }
}
