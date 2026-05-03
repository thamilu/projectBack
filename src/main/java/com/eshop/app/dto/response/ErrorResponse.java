package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error response DTO for REST API.
 * 
 * <p>Provides consistent error structure across all endpoints with:
 * <ul>
 *   <li>HTTP status code and error name</li>
 *   <li>Human-readable error message</li>
 *   <li>Request path that caused the error</li>
 *   <li>Timestamp in ISO-8601 format</li>
 *   <li>Optional field-level validation errors</li>
 *   <li>Optional additional details</li>
 *   <li>Optional error tracking ID</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * HTTP status code (e.g., 400, 404, 500).
     */
    private int status;
    
    /**
     * Error type/name (e.g., "Bad Request", "Not Found").
     */
    private String error;
    
    /**
     * Human-readable error message.
     */
    private String message;

    /**
     * Application specific error code (e.g., USER_NOT_FOUND)
     */
    private String errorCode;
    
    /**
     * Request path that caused the error.
     */
    private String path;
    
    /**
     * Error timestamp in ISO-8601 format.
     */
    private Instant timestamp;
    
    /**
     * Field-level validation errors (field name -> error message).
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Additional error details (e.g., productId, requested, available).
     */
    private Map<String, Object> details;
    
    /**
     * Unique error tracking ID for support/debugging.
     */
    private String errorId;
    
    /**
     * Retry-After header value (for rate limiting).
     */
    private Integer retryAfter;

    /**
     * Factory method to create a simple error response.
     */
    public static ErrorResponse of(int status, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .error(getErrorName(status))
            .message(message)
            .path(path)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Factory method with explicit error name and error code.
     */
    public static ErrorResponse of(int status, String error, String errorCode, String message, String path) {
        return ErrorResponse.builder()
            .status(status)
            .error(error)
            .errorCode(errorCode)
            .message(message)
            .path(path)
            .timestamp(Instant.now())
            .build();
    }

    /**
     * Maps HTTP status code to error name.
     */
    private static String getErrorName(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 413 -> "Payload Too Large";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "Error";
        };
    }
}
