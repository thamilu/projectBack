package com.eshop.app.user.infrastructure.keycloak;

/**
 * Unchecked exception thrown when a call to the Keycloak Admin REST API fails at the
 * transport/HTTP level (e.g. connectivity failure, non-2xx response).
 *
 * <p>This exception exists to prevent vendor-specific JAX-RS exceptions
 * ({@link jakarta.ws.rs.WebApplicationException}, {@link jakarta.ws.rs.ProcessingException})
 * from leaking out of the {@code infrastructure.keycloak} package into application/domain
 * layers or API responses.
 */
public class KeycloakAdminOperationException extends RuntimeException {

    public KeycloakAdminOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
