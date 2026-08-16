package com.eshop.app.user.application.service;

import com.eshop.app.user.domain.exception.KeycloakErrorType;
import com.eshop.app.user.domain.exception.KeycloakOperationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.WebApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enterprise Exception Translator for Keycloak client operations. Maps JAX-RS
 * WebApplicationException and other checked exceptions to unified domain-level
 * KeycloakOperationException.
 */
@Component
@Slf4j
public class KeycloakExceptionTranslator {

    public KeycloakOperationException translate(Exception e, String operation, String context) {
        log.error("Keycloak [{}] failed ctx=[{}]: {}", operation, context, e.getMessage(), e);

        if (e instanceof NotFoundException) {
            return new KeycloakOperationException(
                    KeycloakErrorType.USER_NOT_FOUND, "Resource not found during " + operation, e);
        }
        if (e instanceof NotAuthorizedException || e instanceof ForbiddenException) {
            return new KeycloakOperationException(
                    KeycloakErrorType.UNAUTHORIZED, "Auth failure during " + operation, e);
        }
        if (e instanceof ServerErrorException) {
            return new KeycloakOperationException(
                    KeycloakErrorType.SERVER_ERROR, "Keycloak 5xx during " + operation, e);
        }
        if (e instanceof WebApplicationException wae) {
            return new KeycloakOperationException(
                    KeycloakErrorType.SERVER_ERROR,
                    "Keycloak HTTP [" + wae.getResponse().getStatus() + "] during " + operation,
                    e);
        }
        return new KeycloakOperationException(
                KeycloakErrorType.SERVER_ERROR, "Unexpected error during " + operation, e);
    }
}
