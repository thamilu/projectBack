// src/main/java/com/eshop/app/common/exception/ProblemDetailExceptionHandler.java
package com.eshop.app.common.exception;

import com.eshop.app.exception.DuplicateResourceException;
import com.eshop.app.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@Order(1)  // Higher priority
public class ProblemDetailExceptionHandler {

    /**
     * 409 CONFLICT - Duplicate resource (SKU, email, etc.)
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(
        DuplicateResourceException ex,
        HttpServletRequest request) {
        
    log.warn("Duplicate resource: {}", ex.getMessage());
        
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT,
        ex.getMessage()
    );
    problem.setTitle("Resource Already Exists");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now().toString());
        
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * 404 NOT FOUND - Resource not found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
        ResourceNotFoundException ex,
        HttpServletRequest request) {
        
    log.warn("Resource not found: {}", ex.getMessage());
        
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.NOT_FOUND,
        ex.getMessage()
    );
    problem.setTitle("Resource Not Found");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now().toString());
        
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * 400 BAD REQUEST - Validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationErrors(
        MethodArgumentNotValidException ex,
        HttpServletRequest request) {
        
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );
        
    log.warn("Validation failed: {}", errors);
        
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        "Validation failed"
    );
    problem.setTitle("Validation Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("errors", errors);
        
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 400 BAD REQUEST - Validation exception
     */
    @ExceptionHandler(com.eshop.app.exception.ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            com.eshop.app.exception.ValidationException ex,
            HttpServletRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage());
        problem.setTitle("Validation Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 400 BAD REQUEST - Illegal argument
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(
        IllegalArgumentException ex,
        HttpServletRequest request) {
        
    log.warn("Bad request: {}", ex.getMessage());
        
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.BAD_REQUEST,
        ex.getMessage()
    );
    problem.setTitle("Bad Request");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now().toString());
        
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * 500 INTERNAL SERVER ERROR - Catch all (MUST BE LAST)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(
        AccessDeniedException ex,
        HttpServletRequest request) {

        log.warn("Access denied: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.FORBIDDEN,
            ex.getMessage()
        );
        problem.setTitle("Forbidden");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler({AuthenticationException.class, OAuth2AuthenticationException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> handleAuthenticationFailure(
        Exception ex,
        HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "Authentication failed: " + ex.getMessage()
        );
        problem.setTitle("Unauthorized");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(
        Exception ex,
        HttpServletRequest request) {
        
    log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Server error: " + ex.getMessage()
    );
    problem.setTitle("Internal Server Error");
    problem.setInstance(URI.create(request.getRequestURI()));
    problem.setProperty("timestamp", Instant.now().toString());
    problem.setProperty("exception", ex.getClass().getSimpleName());
        
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }
}
