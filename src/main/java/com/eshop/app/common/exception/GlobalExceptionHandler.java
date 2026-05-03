package com.eshop.app.common.exception;

import com.eshop.app.auth.exception.InvalidRedirectUriException;
import com.eshop.app.auth.exception.TooManyRequestsException;
import com.eshop.app.common.constants.HttpHeaderNames;
import com.eshop.app.dto.response.ApiResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import com.eshop.app.exception.ResourceNotFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Global exception handler for all controllers.
 * Provides consistent error responses across the application.
 * NOTE: This class is kept for reference but not active. 
 * The active handler is com.eshop.app.exception.GlobalExceptionHandler
 */
@Slf4j
// @RestControllerAdvice - Disabled to avoid bean conflict with com.eshop.app.exception.GlobalExceptionHandler
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatus(
            ResponseStatusException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Response status exception: {} for path: {}", 
                 correlationId, ex.getReason(), request.getRequestURI());
        
        return ResponseEntity
            .status(ex.getStatusCode())
            .body(ApiResponse.error(ex.getReason()));
    }
    
    @ExceptionHandler({
        com.eshop.app.auth.exception.UnauthorizedException.class,
        com.eshop.app.exception.UnauthorizedException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
            RuntimeException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Unauthorized access attempt to {}: {}", 
                 correlationId, request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Authentication failed for {}: {}", 
                 correlationId, request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Authentication failed: " + ex.getMessage()));
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Access denied for {}: {}", 
                 correlationId, request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Access denied: Insufficient permissions"));
    }
    
    @ExceptionHandler(InvalidRedirectUriException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidRedirectUri(
            InvalidRedirectUriException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Invalid redirect URI from {}: {}", 
                 correlationId, request.getRemoteAddr(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler({TooManyRequestsException.class, RequestNotPermitted.class})
    public ResponseEntity<ApiResponse<Void>> handleRateLimited(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Rate limit exceeded for {} from {}", 
                 correlationId, request.getRequestURI(), request.getRemoteAddr());
        
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaderNames.RETRY_AFTER, "60")
            .body(ApiResponse.error("Rate limit exceeded. Please try again later."));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        
        String correlationId = MDC.get("correlationId");
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("[{}] Validation failed: {}", correlationId, errors);
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Validation failed", errors));
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Invalid argument for {}: {}", 
                 correlationId, request.getRequestURI(), ex.getMessage());
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("Invalid request: " + ex.getMessage()));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        String correlationId = MDC.get("correlationId");
        log.warn("[{}] Resource not found for {}: {}", correlationId, request.getRequestURI(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        
        log.error("[{}] Unhandled exception for {}: {}", 
                  correlationId, request.getRequestURI(), ex.getMessage(), ex);
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("X-Correlation-Id", correlationId)
            .body(ApiResponse.error("An unexpected error occurred. Reference: " + correlationId));
    }
}
