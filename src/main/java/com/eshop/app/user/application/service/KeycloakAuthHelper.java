package com.eshop.app.user.application.service;

import com.eshop.app.core.exception.infrastructure.KeycloakException;
import com.eshop.app.user.api.request.LoginRequest;
import com.eshop.app.user.api.request.RefreshTokenRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Helper component for Keycloak authentication operations. Centralizes request context
 * extraction, PII masking, client error sanitization, and request validation for
 * {@link KeycloakAuthService}.
 *
 * <p>Used as the single error-mapping path for every outbound Keycloak call in that
 * service via {@link #mapToKeycloakException(Throwable, Operation)}: {@code WebClient}'s
 * default {@code retrieve()} behavior already throws {@link WebClientResponseException}
 * for any 4xx/5xx response with no need for a custom {@code onStatus(...)} handler, so
 * each call site only needs {@code .onErrorMap(error -> mapToKeycloakException(error,
 * OPERATION))} at the end of its reactive chain.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KeycloakAuthHelper {

    /**
     * OAuth2 operation context, used to select an operation-appropriate message from
     * {@link #parseErrorMessage(String, Operation)} (e.g. an {@code invalid_grant}
     * response means "wrong password" for {@link #PASSWORD_GRANT} but "session expired"
     * for {@link #REFRESH_TOKEN}). A compile-time enum rather than a raw string: a typo
     * fails to compile instead of silently falling through to the generic default
     * message, and the compiler flags every call site if a new operation is ever added.
     */
    public enum Operation {
        PASSWORD_GRANT,
        REFRESH_TOKEN,
        AUTHORIZATION_CODE,
        USER_INFO,
        INTROSPECT,
        LOGOUT,
        OPENID_CONFIGURATION,
        JWKS,
        CLIENT_CREDENTIALS
    }

    /** JSON field names known to carry credential material in OAuth2/Keycloak response
     *  bodies. Redaction targets these by structural field name (see
     *  {@link #sanitizeErrorBody}) rather than by regex pattern-matching against raw
     *  text, so it isn't defeated by JSON whitespace variance or an empty field value. */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "access_token", "refresh_token", "client_secret", "password", "id_token");

    private final ObjectMapper objectMapper;

    /**
     * Masks an email address for secure logging. Example: john.doe@example.com -> j***@example.com
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***";
        }
        String[] parts = email.split("@", 2);
        if (parts[0].isEmpty()) {
            return "*@" + parts[1];
        }
        return parts[0].charAt(0) + "***@" + parts[1];
    }

    /**
     * Sanitizes a Keycloak error response body by structurally redacting fields known to
     * carry credential material, so it's safe to write to logs. Parses the body as JSON
     * and redacts by field name — correct regardless of whitespace or an empty field
     * value, unlike a regex-based approach matching literal {@code "key":"value"} text.
     * Falls back to a safe placeholder for a non-JSON body (e.g. an HTML error page from
     * a reverse proxy in front of Keycloak).
     */
    public String sanitizeErrorBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isObject()) {
                return root.toString();
            }
            ObjectNode sanitized = ((ObjectNode) root).deepCopy();
            for (String field : SENSITIVE_FIELDS) {
                if (sanitized.has(field)) {
                    sanitized.put(field, "[REDACTED]");
                }
            }
            return objectMapper.writeValueAsString(sanitized);
        } catch (Exception e) {
            return "[non-JSON error body omitted from logs]";
        }
    }

    /** Parses the Keycloak error response body and returns a user-friendly message. */
    public String parseErrorMessage(String errorBody, Operation operation) {
        if (errorBody == null || errorBody.isBlank()) {
            return "Authentication failed";
        }
        try {
            JsonNode errorNode = objectMapper.readTree(errorBody);
            String errorType = errorNode.path("error").asText();

            return switch (errorType) {
                case "invalid_grant" ->
                        switch (operation) {
                            case PASSWORD_GRANT -> "Invalid email or password";
                            case REFRESH_TOKEN -> "Session expired. Please login again.";
                            case AUTHORIZATION_CODE -> "Invalid or expired authorization code";
                            default -> "Authentication failed";
                        };
                case "invalid_client" -> "Invalid client configuration";
                case "unauthorized_client" -> "Application not authorized";
                case "unsupported_grant_type" -> "Unsupported authentication method";
                case "invalid_scope" -> "Invalid permission scope requested";
                default -> "Authentication failed. Please try again.";
            };
        } catch (Exception e) {
            return "Authentication failed";
        }
    }

    public void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request cannot be null");
        }
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
    }

    public void validateRefreshTokenRequest(RefreshTokenRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Refresh token request cannot be null");
        }
        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
    }

    public void validateAuthorizationCodeRequest(String code, String redirectUri) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Authorization code is required");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new IllegalArgumentException("Redirect URI is required");
        }
    }

    public void validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token is required");
        }
    }

    /**
     * Maps errors from WebClient and Resilience4j into {@link KeycloakException}, so every
     * call site in {@link KeycloakAuthService} ends its reactive chain with the same
     * exception type carrying an accurate HTTP status and a curated, operation-aware
     * message.
     *
     * @param error     the raw error signaled by the reactive chain
     * @param operation the OAuth2 operation context, used to select an operation-appropriate
     *                  message from {@link #parseErrorMessage}
     */
    public Throwable mapToKeycloakException(Throwable error, Operation operation) {
        if (error instanceof KeycloakException) {
            return error;
        }
        if (error instanceof CallNotPermittedException) {
            log.warn("Keycloak circuit breaker open for operation={}", operation);
            return new KeycloakException(
                    "Authentication service temporarily unavailable. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE,
                    error);
        }
        if (error instanceof TimeoutException) {
            log.error("Keycloak call timed out for operation={}", operation);
            // GATEWAY_TIMEOUT (504), not REQUEST_TIMEOUT (408): 408 signals the CLIENT was
            // too slow sending its request, which is wrong here — this is our backend
            // timing out while WAITING on Keycloak, an upstream/server-side failure.
            return new KeycloakException(
                    "Authentication request timed out. Please try again.",
                    HttpStatus.GATEWAY_TIMEOUT,
                    error);
        }
        if (error instanceof WebClientResponseException webEx) {
            String rawBody = webEx.getResponseBodyAsString();
            boolean isUpstreamServerError = webEx.getStatusCode().is5xxServerError();

            // A Keycloak 5xx must not masquerade as THIS service's own internal error —
            // from this service's perspective, an upstream dependency failing is a 503
            // (Service Unavailable), not a 500/502/504 that would misleadingly suggest a
            // bug in this backend and pollute this service's own 5xx error-rate metrics
            // with a dependency's failures. 4xx codes (400/401/403/404) carry meaningfully
            // different semantics from Keycloak and ARE passed through as-is — collapsing
            // them all to a single status (e.g. always 401) would destroy that distinction.
            HttpStatus status;
            if (isUpstreamServerError) {
                status = HttpStatus.SERVICE_UNAVAILABLE;
            } else {
                HttpStatus resolved = HttpStatus.resolve(webEx.getStatusCode().value());
                status = resolved != null ? resolved : HttpStatus.BAD_REQUEST;
            }

            if (isUpstreamServerError) {
                log.error("Keycloak call failed for operation={}: upstream status={}", operation, webEx.getStatusCode());
            } else {
                log.warn("Keycloak call rejected for operation={}: upstream status={}", operation, webEx.getStatusCode());
            }
            log.debug("Keycloak error response body (sanitized): {}", sanitizeErrorBody(rawBody));

            String message = parseErrorMessage(rawBody, operation);
            return new KeycloakException(message, status, error);
        }
        log.error("Unexpected error calling Keycloak for operation={}", operation, error);
        return new KeycloakException(
                "Authentication failed due to unexpected error",
                HttpStatus.INTERNAL_SERVER_ERROR,
                error);
    }
}
