package com.eshop.app.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized API Error Response (Java Record)
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Consistent error structure across all endpoints</li>
 *   <li>Trace ID for debugging and log correlation</li>
 *   <li>Machine-readable error codes</li>
 *   <li>Validation error details</li>
 *   <li>Production-safe (no stack traces)</li>
 * </ul>
 * 
 * @param code Machine-readable error code (e.g., "VALIDATION_ERROR", "UNAUTHORIZED")
 * @param message Human-readable error message
 * @param timestamp When the error occurred
 * @param traceId Correlation ID for log tracking
 * @param path Request path where error occurred
 * @param validationErrors Field-level validation errors (if applicable)
 * @param metadata Additional context-specific error data
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response with trace correlation")
public record ApiError(
    @Schema(description = "Machine-readable error code", example = "UNAUTHORIZED")
    String code,
    
    @Schema(description = "Human-readable error message", example = "Authentication required")
    String message,
    
    @Schema(description = "Error occurrence timestamp")
    Instant timestamp,
    
    @Schema(description = "Trace ID for log correlation", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
    String traceId,
    
    @Schema(description = "Request path", example = "/api/v1/dashboard/admin")
    String path,
    
    @Schema(description = "Field-level validation errors")
    List<ValidationError> validationErrors,
    
    @Schema(description = "Additional error metadata")
    Map<String, Object> metadata
) {
    /**
     * Validation Error Detail
     */
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValidationError(
        @Schema(description = "Field name", example = "email")
        String field,
        
        @Schema(description = "Rejected value", example = "invalid-email")
        Object rejectedValue,
        
        @Schema(description = "Error message", example = "Must be a valid email address")
        String message
    ) {}
    
    /**
     * Compact constructor with defaults
     */
    public ApiError {
        timestamp = timestamp != null ? timestamp : Instant.now();
        validationErrors = validationErrors != null ? List.copyOf(validationErrors) : null;
        metadata = metadata != null ? Map.copyOf(metadata) : null;
    }
}
