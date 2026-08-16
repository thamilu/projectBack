package com.eshop.app.core.exception.handler;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.core.exception.infrastructure.ServiceTimeoutException;
import com.eshop.app.core.exception.security.UnauthorizedException;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import org.springframework.core.annotation.Order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for Dashboard Controllers
 *
 * <p>Handles all exceptions thrown by dashboard endpoints and provides
 * consistent error responses across the application.</p>
 */
@RestControllerAdvice(basePackages = "com.eshop.app.controller")
@Order(0)
@Slf4j
public class DashboardExceptionHandler {

    /**
     * Handle Validation exceptions raised explicitly by the business layer.
     * Must be declared so Spring can dispatch to it in preference to the
     * more generic {@link #handleBusinessException} handler, ensuring
     * field-level validation errors are not silently discarded.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException ex, WebRequest request) {

        log.warn("Validation error [{}]: {} (code: {})",
                request.getDescription(false), ex.getMessage(), ex.getErrorCode());

        String message = ex.getMessage();
        if (!ex.getFieldErrors().isEmpty()) {
            String details = ex.getFieldErrors().stream()
                    .map(fe -> fe.getField() + ": " + fe.getMessage())
                    .collect(Collectors.joining("; "));
            message = message + " [" + details + "]";
        }

        return ResponseEntity
                .status(ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    /**
     * Handle Business specific exceptions
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("Business logic error [{}]: {} (code: {})",
                request.getDescription(false), ex.getMessage(), ex.getErrorCode());

        return ResponseEntity
                .status(ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle Service Timeout exceptions
     */
    @ExceptionHandler(ServiceTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTimeoutException(
            ServiceTimeoutException ex, WebRequest request) {

        log.error("Service timeout [{}]: {}", request.getDescription(false), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.REQUEST_TIMEOUT)
                .body(ApiResponse.error("Request timed out: " + ex.getMessage()));
    }

    /**
     * Handle Unauthorized exceptions.
     * Client-facing message is intentionally generic to avoid leaking
     * internal authentication/authorization details.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Unauthorized access attempt [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Unauthorized: authentication is required to access this resource."));
    }

    /**
     * Handle Resource Not Found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Resource not found: " + ex.getMessage()));
    }

    /**
     * Handle Access Denied exceptions.
     * Client-facing message is intentionally generic to avoid leaking
     * internal authorization details.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Access denied: you do not have permission to perform this action."));
    }

    /**
     * Handle Bean Validation exceptions raised for @Valid request bodies.
     * Preserves every validation message per field (a field may fail
     * multiple constraints simultaneously).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, List<String>> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.computeIfAbsent(fieldError.getField(), key -> new ArrayList<>())
                    .add(fieldError.getDefaultMessage());
        }

        log.warn("Validation failed [{}]: {}", request.getDescription(false), errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(formatFieldErrors("Validation failed", errors)));
    }

    /**
     * Handle Constraint Violation exceptions raised for @Validated
     * method/path/query parameters.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, List<String>> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.computeIfAbsent(violation.getPropertyPath().toString(), key -> new ArrayList<>())
                        .add(violation.getMessage())
        );

        log.warn("Constraint violation [{}]: {}", request.getDescription(false), errors);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(formatFieldErrors("Invalid request parameters", errors)));
    }

    /**
     * Handle malformed/unreadable request bodies (e.g., invalid JSON).
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {

        log.warn("Malformed request body [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Malformed request body. Please verify the request payload."));
    }

    /**
     * Handle request parameters/path variables with an incompatible type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Type mismatch [{}]: parameter '{}' should be of type '{}'",
                request.getDescription(false), ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(String.format("Invalid value for parameter '%s'.", ex.getName())));
    }

    /**
     * Handle missing required request parameters.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {

        log.warn("Missing request parameter [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(String.format("Required parameter '%s' is missing.", ex.getParameterName())));
    }

    /**
     * Handle unsupported HTTP methods on an existing endpoint.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, WebRequest request) {

        log.warn("Method not supported [{}]: {}", request.getDescription(false), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error occurred [{}]: {}", request.getDescription(false), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }

    /**
     * Formats a field-name-to-messages map into a single human-readable
     * string for the current {@code ApiResponse.error(String)} contract.
     */
    private String formatFieldErrors(String prefix, Map<String, List<String>> fieldErrors) {
        String details = fieldErrors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + String.join(", ", entry.getValue()))
                .collect(Collectors.joining("; "));
        return prefix + ": " + details;
    }
}
