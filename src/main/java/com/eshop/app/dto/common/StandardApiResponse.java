package com.eshop.app.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Sealed interface for standardized API responses.
 * 
 * <p>Java 21 Pattern: Sealed types provide compile-time exhaustiveness checking
 * and explicit type hierarchy control.
 * 
 * <h2>Benefits:</h2>
 * <ul>
 *   <li>Type safety: Only permitted subtypes allowed</li>
 *   <li>Pattern matching: Exhaustive switch expressions</li>
 *   <li>Clear contracts: Explicit response type hierarchy</li>
 * </ul>
 * 
 * @author EShop Team
 * @version 1.0
 * @since 2025-12-20
 */
@Schema(description = "Standardized API response wrapper")
public sealed interface StandardApiResponse permits
    StandardApiResponse.Success,
    StandardApiResponse.Error,
    StandardApiResponse.ValidationError {
    
    /**
     * Success response with data payload.
     * 
     * @param <T> type of response data
     * @param success always true for success responses
     * @param message human-readable success message
     * @param data response payload
     * @param timestamp response generation timestamp
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Successful API response with data")
    record Success<T>(
        @Schema(description = "Success indicator", example = "true")
        boolean success,
        
        @Schema(description = "Success message", example = "Operation completed successfully")
        String message,
        
        @Schema(description = "Response data payload")
        T data,
        
        @Schema(description = "Response timestamp", example = "2025-12-20T10:15:30Z")
        Instant timestamp
    ) implements StandardApiResponse {
        
        public Success {
            // Compact constructor - validation
            if (message == null || message.isBlank()) {
                message = "Success";
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }
        
        /**
         * Create success response with data
         */
        public static <T> Success<T> of(T data) {
            return new Success<>(true, "Success", data, Instant.now());
        }
        
        /**
         * Create success response with custom message
         */
        public static <T> Success<T> of(String message, T data) {
            return new Success<>(true, message, data, Instant.now());
        }
    }
    
    /**
     * Error response for business logic errors.
     * 
     * @param success always false for error responses
     * @param message human-readable error message
     * @param errorCode application-specific error code
     * @param timestamp error timestamp
     * @param path request path where error occurred
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Error API response")
    record Error(
        @Schema(description = "Success indicator", example = "false")
        boolean success,
        
        @Schema(description = "Error message", example = "Resource not found")
        String message,
        
        @Schema(description = "Error code", example = "RESOURCE_NOT_FOUND")
        String errorCode,
        
        @Schema(description = "Error timestamp", example = "2025-12-20T10:15:30Z")
        Instant timestamp,
        
        @Schema(description = "Request path", example = "/api/v1/products/999")
        String path
    ) implements StandardApiResponse {
        
        public Error {
            if (message == null || message.isBlank()) {
                message = "An error occurred";
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
        }
        
        /**
         * Create error response
         */
        public static Error of(String message, String errorCode) {
            return new Error(false, message, errorCode, Instant.now(), null);
        }
        
        /**
         * Create error response with path
         */
        public static Error of(String message, String errorCode, String path) {
            return new Error(false, message, errorCode, Instant.now(), path);
        }
    }
    
    /**
     * Validation error response with field-level errors.
     * 
     * @param success always false for validation errors
     * @param message human-readable error message
     * @param errors map of field names to error messages
     * @param timestamp error timestamp
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Validation error API response")
    record ValidationError(
        @Schema(description = "Success indicator", example = "false")
        boolean success,
        
        @Schema(description = "Error message", example = "Validation failed")
        String message,
        
        @Schema(description = "Field validation errors")
        java.util.Map<String, String> errors,
        
        @Schema(description = "Error timestamp", example = "2025-12-20T10:15:30Z")
        Instant timestamp
    ) implements StandardApiResponse {
        
        public ValidationError {
            if (message == null || message.isBlank()) {
                message = "Validation failed";
            }
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            if (errors == null) {
                errors = java.util.Map.of();
            }
        }
        
        /**
         * Create validation error response
         */
        public static ValidationError of(java.util.Map<String, String> errors) {
            return new ValidationError(false, "Validation failed", errors, Instant.now());
        }
        
        /**
         * Create validation error response with custom message
         */
        public static ValidationError of(String message, java.util.Map<String, String> errors) {
            return new ValidationError(false, message, errors, Instant.now());
        }
    }
    
    /**
     * Pattern matching helper for response handling.
     * 
     * <p>Java 21 Pattern Matching Example:
     * <pre>
     * StandardApiResponse response = // ... get response
     * String result = switch (response) {
     *     case Success(var success, var msg, var data, var ts) -> 
     *         "Got data: " + data;
     *     case Error(var success, var msg, var code, var ts, var path) -> 
     *         "Error: " + msg;
     *     case ValidationError(var success, var msg, var errors, var ts) -> 
     *         "Validation failed: " + errors;
     * };
     * </pre>
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }
    
    default boolean isError() {
        return this instanceof Error || this instanceof ValidationError;
    }
}
