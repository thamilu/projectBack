package com.eshop.app.user.domain.exception;

/** Enum identifying specific categories of Keycloak errors for structured response handling. */
public enum KeycloakErrorType {
    USER_NOT_FOUND,
    ROLE_NOT_FOUND,
    UNAUTHORIZED,
    SERVER_ERROR,
    NETWORK_ERROR,
    INVALID_REQUEST
}
