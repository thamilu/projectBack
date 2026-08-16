package com.eshop.app.core.api.handler;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.exception.infrastructure.KeycloakException;
import com.eshop.app.core.util.CorrelationIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handling for Keycloak/authentication-service failures.
 *
 * <p>Servlet MVC advice (matches {@code GlobalExceptionHandler}) — this app runs on the Servlet
 * stack, not WebFlux, even though {@code spring-boot-starter-webflux} is present for {@code
 * WebClient} usage in Keycloak integration services.
 *
 * <p>Deliberately left <b>unscoped</b> (no {@code assignableTypes}/{@code basePackages}):
 * {@link KeycloakException} is thrown from more than just the auth controller —
 * {@code SellerAdminServiceImpl} also throws it synchronously on Keycloak role-sync failures.
 * Scoping this advice to auth controllers only would silently drop that path back to
 * {@code GlobalExceptionHandler}'s generic catch-all, losing the curated message/error code.
 * Precedence over that catch-all is instead guaranteed via {@code @Order(HIGHEST_PRECEDENCE)}.
 *
 * <p>{@code KeycloakAuthController}'s login/refresh/callback/userinfo endpoints let errors
 * propagate here (no {@code onErrorResume} swallowing) so failures produce a structured
 * {@link ApiError} with correlation ID/error code instead of an empty-body status. Its
 * {@code introspect}/{@code config} endpoints never wrapped errors and already reached this
 * advice. Its {@code logout} endpoint intentionally still swallows errors — logout is
 * best-effort revocation for a stateless JWT setup, so a Keycloak-side failure should not
 * block the client from completing logout.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthControllerExceptionHandler {

    private static final String TIMEOUT_ERROR_CODE = "AUTH_SERVICE_TIMEOUT";
    private static final String GENERIC_SERVER_ERROR_MESSAGE =
            "Authentication service is temporarily unavailable. Please try again later.";

    @ExceptionHandler(KeycloakException.class)
    public ResponseEntity<ApiError> handleKeycloakException(
            KeycloakException ex, HttpServletRequest request) {

        String correlationId = CorrelationIdUtils.getCurrent();
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;
        boolean isServerError = status.is5xxServerError();

        if (isServerError) {
            log.error(
                    "Keycloak error — correlationId=[{}] status=[{}] errorCode=[{}]",
                    correlationId,
                    status,
                    ex.getErrorCode(),
                    ex);
        } else {
            log.warn(
                    "Keycloak error — correlationId=[{}] status=[{}] errorCode=[{}] message=[{}]",
                    correlationId,
                    status,
                    ex.getErrorCode(),
                    ex.getMessage());
        }

        // Never leak internal failure details (connection/downstream info) for server-side
        // failures; 4xx messages are expected to already be curated/client-safe by
        // KeycloakException itself.
        String clientMessage = isServerError ? GENERIC_SERVER_ERROR_MESSAGE : ex.getMessage();

        ApiError error = buildApiError(
                status, status.getReasonPhrase(), ex.getErrorCode(), clientMessage, request, correlationId);

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ApiError> handleTimeout(TimeoutException ex, HttpServletRequest request) {
        String correlationId = CorrelationIdUtils.getCurrent();
        log.error("Request timeout — correlationId=[{}]", correlationId, ex);

        ApiError error = buildApiError(
                HttpStatus.GATEWAY_TIMEOUT,
                HttpStatus.GATEWAY_TIMEOUT.getReasonPhrase(),
                TIMEOUT_ERROR_CODE,
                "Authentication service did not respond in time",
                request,
                correlationId);

        return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(error);
    }

    private ApiError buildApiError(
            HttpStatus status,
            String errorReason,
            String errorCode,
            String message,
            HttpServletRequest request,
            String correlationId) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(errorReason)
                .errorCode(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId)
                .build();
    }
}
