
package com.eshop.app.exception;

import com.eshop.app.dto.response.ApiError;
import com.eshop.app.dto.response.ErrorResponse;
import com.eshop.app.dto.response.ValidationErrorResponse;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Global Exception Handler
 * <p>
 * Provides comprehensive, consistent error handling across all API endpoints.
 * Follows RFC 7807 Problem Details with additional metadata for client handling.
 * <p>
 * Exception Hierarchy:
 * <ul>
 *   <li><b>Business Exceptions:</b> Domain-specific errors (400-409)</li>
 *   <li><b>Validation Exceptions:</b> Input validation failures (400)</li>
 *   <li><b>Security Exceptions:</b> Authentication/Authorization (401, 403)</li>
 *   <li><b>External Service Exceptions:</b> Third-party failures (502, 503)</li>
 *   <li><b>Database Exceptions:</b> Data integrity, optimistic locking (409, 500)</li>
 *   <li><b>Rate Limiting:</b> Too many requests (429)</li>
 * </ul>
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
        
        // ═══════════════════════════════════════════════════════════════
        // Rate Limiting Exceptions
        // ═══════════════════════════════════════════════════════════════
        
        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ApiError> handleRateLimitExceeded(
                RateLimitExceededException ex, HttpServletRequest request) {
                log.warn("Rate limit exceeded: limiter={}, key={}", 
                        ex.getLimiterName(), ex.getKey());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("RATE_LIMIT_EXCEEDED")
                        .build();
                
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("X-RateLimit-Retry-After", "60")
                        .body(error);
        }
        
        @ExceptionHandler(RequestNotPermitted.class)
        public ResponseEntity<ApiError> handleRequestNotPermitted(
                RequestNotPermitted ex, HttpServletRequest request) {
                log.warn("Request not permitted (rate limit or circuit breaker): {}", 
                        ex.getMessage());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.TOO_MANY_REQUESTS.value())
                        .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                        .message("Too many requests. Please try again later.")
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("TOO_MANY_REQUESTS")
                        .build();
                
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .header("X-RateLimit-Retry-After", "60")
                        .body(error);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // Business Exceptions
        // ═══════════════════════════════════════════════════════════════

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
                log.warn("Business exception: {}", ex.getMessage());
                ErrorResponse error = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(ex.getHttpStatus() != null ? ex.getHttpStatus().value() : HttpStatus.BAD_REQUEST.value())
                                .error(ex.getHttpStatus() != null ? ex.getHttpStatus().getReasonPhrase() : "Business Error")
                                .message(ex.getMessage())
                                .errorCode(ex.getErrorCode())
                                .path(getRequestPath(request))
                                .details(ex.getDetails())
                                .build();
                return ResponseEntity.status(ex.getHttpStatus() != null ? ex.getHttpStatus() : HttpStatus.BAD_REQUEST).body(error);
        }

        @ExceptionHandler(ValidationException.class)
        public ResponseEntity<ValidationErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
                log.warn("Validation exception: {}", ex.getMessage());
                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .errorCode(ex.getErrorCode())
                                .message(ex.getMessage())
                                .path(getRequestPath(request))
                                .build();

                ex.getFieldErrors().forEach(fe -> response.addFieldError(fe.getField(), fe.getMessage(), fe.getRejectedValue()));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Validation Failed")
                                .errorCode("VALIDATION_ERROR")
                                .message("Request validation failed")
                                .path(getRequestPath(request))
                                .build();

                ex.getBindingResult().getFieldErrors().forEach(error ->
                                response.addFieldError(error.getField(), error.getDefaultMessage(), error.getRejectedValue())
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(BindException.class)
        public ResponseEntity<ValidationErrorResponse> handleBindException(BindException ex, HttpServletRequest request) {
                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Binding Failed")
                                .errorCode("BINDING_ERROR")
                                .message("Request binding failed")
                                .path(getRequestPath(request))
                                .build();

                ex.getBindingResult().getFieldErrors().forEach(error ->
                                response.addFieldError(error.getField(), error.getDefaultMessage(), error.getRejectedValue())
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.FORBIDDEN.value())
                                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                                .errorCode("ACCESS_DENIED")
                                .message("You don't have permission to access this resource")
                                .path(getRequestPath(request))
                                .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .errorCode("AUTHENTICATION_FAILED")
                                .message("Authentication failed")
                                .path(getRequestPath(request))
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler({
                com.eshop.app.exception.UnauthorizedException.class,
                com.eshop.app.auth.exception.UnauthorizedException.class
        })
        public ResponseEntity<ErrorResponse> handleUnauthorizedException(RuntimeException ex, HttpServletRequest request) {
                log.warn("Unauthorized access attempt: {}", ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.UNAUTHORIZED.value())
                                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                                .errorCode("UNAUTHORIZED")
                                .message(ex.getMessage())
                                .path(getRequestPath(request))
                                .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        @ExceptionHandler(MaxUploadSizeExceededException.class)
        public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
                ErrorResponse errorResponse = ErrorResponse.builder()
                                                                .timestamp(Instant.now())
                                                                .status(413)
                                                                .error("Payload Too Large")
                                .errorCode("FILE_TOO_LARGE")
                                .message("File size exceeds maximum allowed limit")
                                .path(getRequestPath(request))
                                .build();
                                return ResponseEntity.status(413).body(errorResponse);
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<ValidationErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
                ValidationErrorResponse response = ValidationErrorResponse.builder()
                                .timestamp(LocalDateTime.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error("Constraint Violation")
                                .errorCode("CONSTRAINT_VIOLATION")
                                .message("Validation constraint violated")
                                .path(getRequestPath(request))
                                .build();

                ex.getConstraintViolations().forEach(violation ->
                                response.addFieldError(violation.getPropertyPath().toString(), violation.getMessage(), null)
                );

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(ResourceAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleResourceAlreadyExists(ResourceAlreadyExistsException ex, HttpServletRequest request) {
                log.warn("Resource already exists: {}", ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .errorCode("RESOURCE_EXISTS")
                                .message(ex.getMessage())
                                .path(getRequestPath(request))
                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        @ExceptionHandler(DuplicateResourceException.class)
        public ResponseEntity<ProblemDetail> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
                log.warn("Duplicate resource: {}", ex.getMessage());

                ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
                problem.setTitle("Resource Already Exists");
                problem.setInstance(URI.create(getRequestPath(request) != null ? getRequestPath(request) : "/"));
                problem.setProperty("timestamp", Instant.now());
                problem.setProperty("resourceName", ex.getResourceName());
                problem.setProperty("fieldName", ex.getFieldName());
                problem.setProperty("fieldValue", ex.getFieldValue());

                return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
                String errorId = java.util.UUID.randomUUID().toString();
                log.warn("Data integrity violation (id={}): {}", errorId, ex.getMessage(), ex);
                ErrorResponse errorResponse = ErrorResponse.builder()
                                                .timestamp(Instant.now())
                                                .status(HttpStatus.CONFLICT.value())
                                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                                .errorCode("DATA_INTEGRITY_VIOLATION")
                                                .message("Database constraint violated. Provide unique/valid data. Error ID: " + errorId)
                                                .errorId(errorId)
                                                .path(getRequestPath(request))
                                                .build();
                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
                log.warn("Illegal argument: {}", ex.getMessage());
                ErrorResponse errorResponse = ErrorResponse.builder()
                                                .timestamp(Instant.now())
                                                .status(HttpStatus.BAD_REQUEST.value())
                                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                                .errorCode("INVALID_ARGUMENT")
                                                .message(ex.getMessage())
                                                .path(getRequestPath(request))
                                                .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // HTTP/Request Exceptions
        // ═══════════════════════════════════════════════════════════════
        
        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ApiError> handleMethodNotSupported(
                HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
                log.warn("HTTP method not supported: {} for path {}", 
                        ex.getMethod(), request.getRequestURI());
                
                String supportedMethods = ex.getSupportedHttpMethods() != null
                        ? ex.getSupportedHttpMethods().stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(", "))
                        : "None";
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                        .message(String.format("Method %s is not supported for this endpoint. Supported methods: %s",
                                ex.getMethod(), supportedMethods))
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("METHOD_NOT_ALLOWED")
                        .build();
                
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                        .header("Allow", supportedMethods)
                        .body(error);
        }
        
        @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
        public ResponseEntity<ApiError> handleNotFound(
                Exception ex, HttpServletRequest request) {
                log.warn("Resource not found: {}", request.getRequestURI());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message("The requested resource was not found")
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("NOT_FOUND")
                        .build();
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
                ResourceNotFoundException ex, HttpServletRequest request) {
                log.warn("Resource not found: {}", ex.getMessage());
                
                ErrorResponse error = ErrorResponse.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                        .message(ex.getMessage())
                        .path(getRequestPath(request))
                        .errorCode("RESOURCE_NOT_FOUND")
                        .build();
                
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        @ExceptionHandler(MissingServletRequestParameterException.class)
        public ResponseEntity<ApiError> handleMissingParameter(
                MissingServletRequestParameterException ex, HttpServletRequest request) {
                log.warn("Missing required parameter: {}", ex.getParameterName());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(String.format("Required parameter '%s' of type '%s' is missing",
                                ex.getParameterName(), ex.getParameterType()))
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("MISSING_PARAMETER")
                        .build();
                
                return ResponseEntity.badRequest().body(error);
        }
        
        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiError> handleTypeMismatch(
                MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
                log.warn("Type mismatch for parameter: {}", ex.getName());
                
                String message = String.format("Parameter '%s' should be of type '%s'",
                        ex.getName(),
                        ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message(message)
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("TYPE_MISMATCH")
                        .build();
                
                return ResponseEntity.badRequest().body(error);
        }
        
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiError> handleMessageNotReadable(
                HttpMessageNotReadableException ex, HttpServletRequest request) {
                log.warn("Malformed JSON request: {}", ex.getMostSpecificCause().getMessage());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                        .message("Malformed JSON request. Please check your request body.")
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("MALFORMED_JSON")
                        .build();
                
                return ResponseEntity.badRequest().body(error);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // Security Exceptions - Enhanced
        // ═══════════════════════════════════════════════════════════════
        
        @ExceptionHandler(JwtException.class)
        public ResponseEntity<ApiError> handleJwtException(
                JwtException ex, HttpServletRequest request) {
                log.warn("JWT validation failed: {}", ex.getMessage());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.UNAUTHORIZED.value())
                        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                        .message("Invalid or expired authentication token")
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("INVALID_TOKEN")
                        .build();
                
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // Database Exceptions - Enhanced
        // ═══════════════════════════════════════════════════════════════
        
        @ExceptionHandler(OptimisticLockingFailureException.class)
        public ResponseEntity<ApiError> handleOptimisticLocking(
                OptimisticLockingFailureException ex, HttpServletRequest request) {
                log.warn("Optimistic locking failure: {}", ex.getMessage());
                
                ApiError error = ApiError.builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.CONFLICT.value())
                        .error(HttpStatus.CONFLICT.getReasonPhrase())
                        .message("The resource was modified by another request. Please refresh and try again.")
                        .path(getRequestPath(request))
                        .correlationId(getCorrelationId())
                        .errorCode("OPTIMISTIC_LOCK_FAILURE")
                        .build();
                
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        
        // ═══════════════════════════════════════════════════════════════
        // Category Seeding Exceptions
        // ═══════════════════════════════════════════════════════════════

        @ExceptionHandler(CategorySeedingException.class)
        public ResponseEntity<ApiError> handleCategorySeedingException(
                        CategorySeedingException ex, HttpServletRequest request) {
                String errorId = java.util.UUID.randomUUID().toString();
                log.error("[errorId={}] Category seeding failed: {}", errorId, ex.getMessage(), ex);

                ApiError error = ApiError.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .message("Category seeding operation failed. Please check logs for details.")
                                .path(getRequestPath(request))
                                .correlationId(getCorrelationId())
                                .errorCode(ex.getErrorCode())
                                .build();

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }

        @ExceptionHandler(DuplicateCategoryException.class)
        public ResponseEntity<ApiError> handleDuplicateCategoryException(
                        DuplicateCategoryException ex, HttpServletRequest request) {
                log.warn("Duplicate category detected: {} under parent: {}",
                                ex.getCategoryName(), ex.getParentPath());

                ApiError error = ApiError.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.CONFLICT.value())
                                .error(HttpStatus.CONFLICT.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(getRequestPath(request))
                                .correlationId(getCorrelationId())
                                .errorCode(ex.getErrorCode())
                                .build();

                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        @ExceptionHandler(InvalidCategoryHierarchyException.class)
        public ResponseEntity<ApiError> handleInvalidCategoryHierarchyException(
                        InvalidCategoryHierarchyException ex, HttpServletRequest request) {
                log.warn("Invalid category hierarchy: {}", ex.getMessage());

                ApiError error = ApiError.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.BAD_REQUEST.value())
                                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                                .message(ex.getMessage())
                                .path(getRequestPath(request))
                                .correlationId(getCorrelationId())
                                .errorCode(ex.getErrorCode())
                                .build();

                return ResponseEntity.badRequest().body(error);
        }

        // ═══════════════════════════════════════════════════════════════
        // Catch-All Handler
        // ═══════════════════════════════════════════════════════════════

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
                String errorId = java.util.UUID.randomUUID().toString();
                log.error("Unexpected error (id={}): {}", errorId, ex.getMessage(), ex);
                ErrorResponse errorResponse = ErrorResponse.builder()
                                .timestamp(Instant.now())
                                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                                .errorCode("INTERNAL_ERROR")
                                .message("An unexpected error occurred. Please contact support with Reference ID: " + errorId)
                                .errorId(errorId)
                                .path(getRequestPath(request))
                                .build();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
}
        
