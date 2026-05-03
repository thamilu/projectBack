package com.eshop.app.exception;

import com.eshop.app.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for Dashboard Controllers
 * 
 * <p>Handles all exceptions thrown by dashboard endpoints and provides
 * consistent error responses across the application.</p>
 * 
 * @version 1.0
 * @since 2025-12-12
 */
@RestControllerAdvice(basePackages = "com.eshop.app.controller")
@Slf4j
public class DashboardExceptionHandler {

    /**
     * Handle Dashboard-specific exceptions
     */
    @ExceptionHandler(DashboardException.class)
    public ResponseEntity<ApiResponse<Void>> handleDashboardException(
            DashboardException ex, WebRequest request) {
        
        log.error("Dashboard exception occurred: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Dashboard error: " + ex.getMessage()));
    }

    /**
     * Handle Service Timeout exceptions
     */
    @ExceptionHandler(ServiceTimeoutException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceTimeoutException(
            ServiceTimeoutException ex, WebRequest request) {
        
        log.error("Service timeout: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.REQUEST_TIMEOUT)
            .body(ApiResponse.error("Request timed out: " + ex.getMessage()));
    }

    /**
     * Handle Unauthorized exceptions
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        
        log.warn("Unauthorized access attempt: {}", ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Unauthorized: " + ex.getMessage()));
    }

    /**
     * Handle Resource Not Found exceptions
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("Resource not found: " + ex.getMessage()));
    }

    /**
     * Handle Access Denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Access denied: " + ex.getMessage()));
    }

    /**
     * Handle Validation exceptions
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        log.warn("Validation failed: {}", errors);
        
        String errorMessage = "Validation failed: " + errors.toString();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handle Constraint Violation exceptions
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> 
            errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );
        
        log.warn("Constraint violation: {}", errors);
        
        String errorMessage = "Invalid request parameters: " + errors.toString();
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(errorMessage));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
    }
}
