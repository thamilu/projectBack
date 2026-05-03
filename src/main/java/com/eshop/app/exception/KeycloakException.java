package com.eshop.app.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KeycloakException extends RuntimeException {
    
    private final HttpStatus status;
    
    public KeycloakException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    
    public KeycloakException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }
}
