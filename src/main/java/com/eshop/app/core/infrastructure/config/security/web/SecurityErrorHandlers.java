package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.util.CorrelationIdUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.MeterRegistry;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;

/**
 * Standardized security error handlers for entry point (401) and access denied (403) events.
 * Decoupled from the main SecurityConfig to improve maintainability and cohesion.
 *
 * <p>These handlers are invoked by Spring Security's {@code ExceptionTranslationFilter} for
 * denials that occur in the filter chain itself (e.g. URL-pattern authorization rules in
 * {@code SecurityConfig}) — before a request ever reaches a controller. Method-security denials
 * from {@code @PreAuthorize} on controller methods instead surface as exceptions from within
 * {@code DispatcherServlet} and are handled by {@code GlobalExceptionHandler}, which this class
 * mirrors in error-code/message/correlation-id shape so both paths look identical to API
 * consumers.
 *
 * <p>Neither handler logs the authenticated principal's identifier or role names — only a role
 * <em>count</em> — since the Keycloak subject and full authority set are PII/privilege
 * information that would otherwise land in every 403 log line, including routine ones (a
 * bookmarked admin URL, a stale mobile session). {@link CorrelationIdUtils#getCurrent()} already
 * provides the correlation mechanism needed to trace a specific denial back to its request.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityErrorHandlers {

    private static final String CONTENT_TYPE_JSON_UTF8 = "application/json;charset=UTF-8";

    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) -> {
            if (response.isCommitted()) {
                log.debug("[SECURITY] Response already committed for {} {} — cannot write 401",
                        request.getMethod(), request.getRequestURI());
                return;
            }

            String path = request.getRequestURI();
            log.warn("Authentication error on {} {}: {}", request.getMethod(), path, ex.getMessage());
            meterRegistry.counter("security.http.error", "status", "401").increment();

            response.reset();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(CONTENT_TYPE_JSON_UTF8);
            // RFC 7235 3.1: a 401 response SHOULD include a WWW-Authenticate challenge.
            response.setHeader("WWW-Authenticate", "Bearer realm=\"eshop-api\"");

            ApiError error = ApiError.builder()
                    .timestamp(Instant.now())
                    .status(401)
                    .error("Unauthorized")
                    .errorCode("AUTHENTICATION_FAILED")
                    .message("Authentication required")
                    .path(path)
                    .correlationId(CorrelationIdUtils.getCurrent())
                    .build();

            writeErrorResponse(response, error, path);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> {
            if (response.isCommitted()) {
                log.debug("[SECURITY] Response already committed for {} {} — cannot write 403",
                        request.getMethod(), request.getRequestURI());
                return;
            }

            String path = request.getRequestURI();
            log.warn(
                    "Access denied: {} {} - {} - reason={}",
                    request.getMethod(),
                    path,
                    describeCurrentPrincipal(),
                    ex.getMessage());
            meterRegistry.counter("security.http.error", "status", "403").increment();

            response.reset();
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(CONTENT_TYPE_JSON_UTF8);

            ApiError error = ApiError.builder()
                    .timestamp(Instant.now())
                    .status(403)
                    .error("Forbidden")
                    .errorCode("ACCESS_DENIED")
                    .message("You do not have permission to perform this action")
                    .path(path)
                    .correlationId(CorrelationIdUtils.getCurrent())
                    .build();

            writeErrorResponse(response, error, path);
        };
    }

    /**
     * Writes the error body, swallowing (at debug level) any {@link IOException} — the most
     * common cause is a client that has already disconnected, which is not actionable and
     * should not generate an error-level log entry or a secondary exception-handler invocation.
     */
    private void writeErrorResponse(HttpServletResponse response, ApiError error, String path) {
        try {
            objectMapper.writeValue(response.getOutputStream(), error);
        } catch (IOException e) {
            log.debug("[SECURITY] Could not write error response for {} (client likely disconnected): {}",
                    path, e.getMessage());
        }
    }

    /**
     * Describes the current principal for the access-denied log line without leaking PII: the
     * Keycloak subject (a persistent user identifier) and role names are omitted; only whether a
     * user is authenticated and how many authorities they hold is logged. Pair with the
     * correlation ID already in the log line to look up the full request if a security
     * investigation needs the actual principal.
     */
    private String describeCurrentPrincipal() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
                return "principal=anonymous";
            }
            return "principal=authenticated roleCount=" + auth.getAuthorities().size();
        } catch (Exception e) {
            log.debug("[SECURITY] Could not read authentication from SecurityContext: {}", e.getMessage());
            return "principal=unknown";
        }
    }
}
