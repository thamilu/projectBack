package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standard API Error Response
 * <p>
 * Provides consistent error structure across all API endpoints following
 * RFC 7807 Problem Details principles with additional metadata.
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API error response")
public record ApiError(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @Schema(description = "Timestamp when the error occurred", example = "2026-01-01T10:15:30.123Z")
    Instant timestamp,
    
    @Schema(description = "HTTP status code", example = "400")
    int status,
    
    @Schema(description = "HTTP status reason phrase", example = "Bad Request")
    String error,
    
    @Schema(description = "Error message", example = "Validation failed for the request")
    String message,
    
    @Schema(description = "Request path that caused the error", example = "/api/v1/products")
    String path,
    
    @Schema(description = "Correlation ID for tracing", example = "550e8400-e29b-41d4-a716-446655440000")
    String correlationId,
    
    @Schema(description = "Error code for client-side handling", example = "VALIDATION_ERROR")
    String errorCode,
    
    @Schema(description = "Unique error instance ID for support", example = "err_abc123")
    String errorId,
    
    @Schema(description = "List of field-level validation errors")
    List<FieldError> fieldErrors,
    
    @Schema(description = "Additional error details")
    Map<String, Object> details
) {
    /**
     * Field-level validation error
     */
    @Builder
    @Schema(description = "Field validation error details")
    public record FieldError(
        @Schema(description = "Field name", example = "email")
        String field,
        
        @Schema(description = "Error message", example = "must be a valid email address")
        String message,
        
        @Schema(description = "Rejected value", example = "invalid-email")
        Object rejectedValue
    ) {}
    
    /**
     * Create a simple error response
     */
    public static ApiError of(int status, String error, String message, String path) {
        return ApiError.builder()
            .timestamp(Instant.now())
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .build();
    }
    
    /**
     * Create an error response with correlation ID
     */
    public static ApiError of(int status, String error, String message, String path, String correlationId) {
        return ApiError.builder()
            .timestamp(Instant.now())
            .status(status)
            .error(error)
            .message(message)
            .path(path)
            .correlationId(correlationId)
            .build();
    }
}
