package com.eshop.app.core.exception.handler;

import com.eshop.app.core.api.response.ApiError;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.security.RateLimitExceededException;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * <p>
 * Provides comprehensive, consistent error handling across all API endpoints.
 * Follows RFC 7807 Problem Details with additional metadata for client handling.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String getRequestPath(HttpServletRequest request) {
        return request != null ? request.getRequestURI() : "/unknown";
    }

    private String getCorrelationId() {
        return Optional.ofNullable(MDC.get("correlationId"))
                .orElse("no-correlation-id");
    }

    private ApiError.ApiErrorBuilder createBaseError(HttpStatus status, String errorCode, String message, HttpServletRequest request) {
        return ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .path(getRequestPath(request))
                .correlationId(getCorrelationId());
    }

    // =========================================================================
    // Rate Limiting Exceptions
    // =========================================================================

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiError> handleRateLimitExceeded(RateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Rate limit exceeded: limiter={}, key={}", ex.getLimiterName(), ex.getKey());
        ApiError error = createBaseError(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("X-RateLimit-Retry-After", "60").body(error);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiError> handleRequestNotPermitted(RequestNotPermitted ex, HttpServletRequest request) {
        log.warn("Request not permitted: {}", ex.getMessage());
        ApiError error = createBaseError(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Too many requests. Please try again later.", request).build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("X-RateLimit-Retry-After", "60").body(error);
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

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiError> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());
        List<ApiError.FieldError> fieldErrors = ex.getFieldErrors().stream()
                .map(fe -> ApiError.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getMessage())
                        .rejectedValue(fe.getRejectedValue())
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
                        .rejectedValue(error.getRejectedValue())
                        .build())
                .collect(Collectors.toList());

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
        ApiError error = createBaseError(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You don't have permission to access this resource", request).build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Authentication failed", request).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwtException(JwtException ex, HttpServletRequest request) {
        log.warn("JWT validation failed: {}", ex.getMessage());
        ApiError error = createBaseError(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Invalid or expired authentication token", request).build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // =========================================================================
    // Resource Exceptions
    // =========================================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // =========================================================================
    // Database Exceptions
    // =========================================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String errorId = java.util.UUID.randomUUID().toString();
        log.error("Data integrity violation (id={}): {}", errorId, ex.getMessage());
        ApiError error = createBaseError(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", 
            "A database constraint was violated. Reference ID: " + errorId, request)
                .errorId(errorId)
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLocking(OptimisticLockingFailureException ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_FAILURE", 
            "The resource was modified by another request. Please refresh and try again.", request).build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // =========================================================================
    // HTTP/Request Exceptions
    // =========================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Malformed JSON request. Please check your request body.", request).build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.NOT_FOUND, "NOT_FOUND", "The requested resource was not found", request).build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        ApiError error = createBaseError(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE", "File size exceeds maximum allowed limit", request).build();
        return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).body(error);
    }

    // =========================================================================
    // Catch-All Handler
    // =========================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        String errorId = java.util.UUID.randomUUID().toString();
        log.error("Unexpected error (id={}): {}", errorId, ex.getMessage(), ex);
        ApiError error = createBaseError(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", 
            "An unexpected error occurred. Reference ID: " + errorId, request)
                .errorId(errorId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
