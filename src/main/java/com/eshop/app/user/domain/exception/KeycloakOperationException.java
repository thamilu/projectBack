package com.eshop.app.user.domain.exception;

/** Domain exception thrown when a Keycloak operation fails. */
public class KeycloakOperationException extends RuntimeException {
    private final KeycloakErrorType errorType;

    public KeycloakOperationException(
            KeycloakErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public KeycloakErrorType getErrorType() {
        return errorType;
    }
}
