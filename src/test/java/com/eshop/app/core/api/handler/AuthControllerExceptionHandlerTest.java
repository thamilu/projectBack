package com.eshop.app.core.api.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.exception.infrastructure.KeycloakException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthControllerExceptionHandlerTest {

    @Mock HttpServletRequest request;

    private final AuthControllerExceptionHandler handler = new AuthControllerExceptionHandler();

    @Test
    void handleKeycloakException_preservesCuratedMessageFor4xx() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        KeycloakException ex = new KeycloakException("Invalid credentials", HttpStatus.UNAUTHORIZED);

        ResponseEntity<ApiError> response = handler.handleKeycloakException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid credentials", response.getBody().message());
        assertEquals("KEYCLOAK_ERROR", response.getBody().errorCode());
        assertEquals("Unauthorized", response.getBody().error());
    }

    @Test
    void handleKeycloakException_sanitizesMessageFor5xx() {
        when(request.getRequestURI()).thenReturn("/api/v1/sellers/1/approve");
        KeycloakException ex = new KeycloakException(
                "Connection refused: keycloak-internal.svc:8443", HttpStatus.INTERNAL_SERVER_ERROR);

        ResponseEntity<ApiError> response = handler.handleKeycloakException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "Authentication service is temporarily unavailable. Please try again later.",
                response.getBody().message());
    }

    @Test
    void handleKeycloakException_fallsBackToInternalServerErrorWhenStatusNull() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        KeycloakException ex = new KeycloakException("boom", null);

        ResponseEntity<ApiError> response = handler.handleKeycloakException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(
                "Authentication service is temporarily unavailable. Please try again later.",
                response.getBody().message());
    }

    @Test
    void handleTimeout_returnsGatewayTimeoutWithErrorCode() {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");
        TimeoutException ex = new TimeoutException("deadline exceeded");

        ResponseEntity<ApiError> response = handler.handleTimeout(ex, request);

        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertEquals("AUTH_SERVICE_TIMEOUT", response.getBody().errorCode());
        assertEquals("Gateway Timeout", response.getBody().error());
        assertNull(response.getBody().fieldErrors());
    }
}
