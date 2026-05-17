package com.eshop.app.core.exception.infrastructure;

import com.eshop.app.core.exception.base.InfrastructureException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class KeycloakException extends InfrastructureException {
    
    private final HttpStatus status;
    
    public KeycloakException(String message, HttpStatus status) {
        super(message, "KEYCLOAK_ERROR");
        this.status = status;
    }
    
    public KeycloakException(String message, HttpStatus status, Throwable cause) {
        super(message, "KEYCLOAK_ERROR", cause);
        this.status = status;
    }
}
