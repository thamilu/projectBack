package com.eshop.app.core.exception.handler;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.security.RateLimitExceededException;
import com.eshop.app.core.util.CorrelationIdUtils;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * <p>
 * Provides comprehensive, consistent error handling across all API endpoints.
 * Follows RFC 7807 Problem Details with additional metadata for client handling.
 * <p>
 * <b>Advice ordering note:</b> This advice is intentionally ordered at
 * {@link Ordered#LOWEST_PRECEDENCE} to act as the application-wide fallback.
 * Any more specific, package-scoped {@code @RestControllerAdvice} (e.g. a
 * dashboard-specific handler) MUST declare an explicit, numerically lower
 * {@code @Order} value to guarantee deterministic precedence over this class
 * for its own package. See review notes for required follow-up.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final String DEFAULT_RETRY_AFTER_SECONDS = "60";

    private static final Set<String> SENSITIVE_FIELD_MARKERS = Set.of(
            "password", "pwd", "secret", "token", "pin", "cvv", "cvc",
            "ssn", "cardnumber", "creditcard", "apikey", "accesskey", "privatekey"
    );

    private static final String REDACTED_VALUE = "***REDACTED***";

    private String getRequestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "/unknown";
    }

    private ApiError.ApiErrorBuilder createBaseError(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(getRequestPath(request))
                .correlationId(CorrelationIdUtils.getCurrent());
    }

    /**
     * Redacts field values whose field name matches a known sensitive-data
     * pattern, preventing user-submitted secrets from being reflected back
     * in validation error responses.
     */
    private Object sanitizeRejectedValue(String fieldName, Object rejectedValue) {
        if (rejectedValue == null || fieldName == null) {
            return rejectedValue;
        }
        String normalizedField = fieldName.toLowerCase(Locale.ROOT);
        boolean sensitive = SENSITIVE_FIELD_MARKERS.stream().anyMatch(normalizedField::contains);
        return sensitive ? REDACTED_VALUE : rejectedValue;
    }

    // =========================================================================
    // Rate Limiting Exceptions
    // =========================================================================

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: limiter={}, key={}", ex.getLimiterName(), ex.getKey());
        ApiError error = createBaseError(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Retry-After", DEFAULT_RETRY_AFTER_SECONDS)
                .body(error);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiError> handleRequestNotPermitted(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Request not permitted: {}", ex.getMessage());
        ApiError error = createBaseError(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Too many requests. Please try again later.", request).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("X-RateLimit-Retry-After", DEFAULT_RETRY_AFTER_SECONDS)
                .body(error);
    }

    // =========================================================================
    // Business & Validation Exceptions
    // =========================================================================

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {}", ex.getMessage());
        HttpStatus status = ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST;
        ApiError error = createBaseError(status, ex.getErrorCode(), ex.getMessage(), request)
                .details(ex.getDetails())
                .build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Invalid argument [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());
        List<ApiError.FieldError> fieldErrors = ex.getFieldErrors().stream()
                .map(fe -> ApiError.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getMessage())
                        .rejectedValue(sanitizeRejectedValue(fe.getField(), fe.getRejectedValue()))
                        .build())
                .collect(Collectors.toList());

        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ApiError.FieldError.builder()
                        .field(error.getField())
                        .message(error.getDefaultMessage())
                        .rejectedValue(sanitizeRejectedValue(error.getField(), error.getRejectedValue()))
                        .build())
                .collect(Collectors.toList());

        log.warn("Method argument validation failed [{}]: {} field error(s)", getRequestPath(request), fieldErrors.size());

        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", request)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<ApiError.FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> ApiError.FieldError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .collect(Collectors.toList());

        log.warn("Constraint violation [{}]: {} violation(s)", getRequestPath(request), fieldErrors.size());

        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "CONSTRAINT_VIOLATION", "Validation constraint violated", request)
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    // =========================================================================
    // Security Exceptions
    // =========================================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You don't have permission to access this resource", request).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Authentication failed", request).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException ex, HttpServletRequest request) {
        log.warn("JWT validation failed [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired authentication token", request).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================================
    // Resource Exceptions
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // =========================================================================
    // Database Exceptions
    // =========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("Data integrity violation (id={}): {}", errorId, ex.getMessage());
        ApiError error = createBaseError(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "A database constraint was violated. Reference ID: " + errorId, request)
                .errorId(errorId)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_FAILURE",
                "The resource was modified by another request. Please refresh and try again.", request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================================
    // HTTP/Request Exceptions
    // =========================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed request body [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request. Please check your request body.", request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch [{}]: parameter='{}' requiredType='{}'", getRequestPath(request), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        String message = String.format("Invalid value for parameter '%s'.", ex.getName());
        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Missing request parameter [{}]: {}", getRequestPath(request), ex.getParameterName());
        String message = String.format("Required parameter '%s' is missing.", ex.getParameterName());
        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", message, request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("HTTP method not supported [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        log.warn("Response status exception [{}]: status={}, reason={}", getRequestPath(request), status, ex.getReason());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        ApiError error = createBaseError(status, "REQUEST_ERROR", message, request).build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        log.warn("Route not found [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found", request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Max upload size exceeded [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "File size exceeds maximum allowed limit", request).build();
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error);
    }

    // =========================================================================
    // External Service / Infrastructure Exceptions
    //
    // NOTE: KeycloakException itself is NOT handled here. com.eshop.app.core.api.handler.
    // AuthControllerExceptionHandler already handles it at @Order(HIGHEST_PRECEDENCE),
    // deliberately unscoped (applies app-wide, not just to auth controllers — see its own
    // Javadoc), with curated 4xx-vs-5xx messaging. A duplicate handler here would never
    // actually run (this advice is @Order(LOWEST_PRECEDENCE)) and would only misdocument
    // this class's real behavior for KeycloakException. Similarly, a bare
    // java.util.concurrent.TimeoutException is already handled there and is intentionally
    // not duplicated below. WebClientResponseException/WebClientRequestException are NOT
    // handled by that class, so they remain here as the app-wide fallback for any
    // WebClient-based integration (e.g. KeycloakAdminService calls that don't wrap
    // failures in KeycloakException) that lets a raw WebClient exception propagate.
    // =========================================================================

    @ExceptionHandler(org.springframework.web.reactive.function.client.WebClientResponseException.class)
    public ResponseEntity<ApiError> handleWebClientResponseException(
            org.springframework.web.reactive.function.client.WebClientResponseException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null || !status.isError()) {
            status = HttpStatus.BAD_GATEWAY;
        }
        log.error("Upstream service call failed [{}]: status={}, body={}",
                getRequestPath(request), ex.getStatusCode(), ex.getMessage());
        String message = status == HttpStatus.NOT_FOUND
                ? "The requested resource was not found on the identity provider"
                : "Upstream identity provider call failed";
        ApiError error = createBaseError(status, "UPSTREAM_SERVICE_ERROR", message, request).build();
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(org.springframework.web.reactive.function.client.WebClientRequestException.class)
    public ResponseEntity<ApiError> handleWebClientRequestException(
            org.springframework.web.reactive.function.client.WebClientRequestException ex, HttpServletRequest request) {
        log.error("Upstream service unreachable [{}]: {}", getRequestPath(request), ex.getMessage());
        ApiError error = createBaseError(HttpStatus.BAD_GATEWAY, "UPSTREAM_UNAVAILABLE",
                "An upstream service is currently unreachable. Please try again.", request).build();
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    // =========================================================================
    // Catch-All Handler
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        String errorId = UUID.randomUUID().toString();
        log.error("Unexpected error (id={}): {}", errorId, ex.getMessage(), ex);
        ApiError error = createBaseError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred. Reference ID: " + errorId, request)
                .errorId(errorId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
