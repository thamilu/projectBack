package com.eshop.app.dto.error;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Standardized error response DTO for all API errors.
 * Provides consistent error structure across the application.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ", timezone = "UTC")
    private Instant timestamp;
    
    private int status;
    private String error;
    private String message;
    private String path;
    private String errorCode;
    private String traceId;
    
    @JsonProperty("field_errors")
    private Map<String, String> fieldErrors;
    
    @JsonProperty("debug_info")
    private DebugInfo debugInfo;
    
    /**
     * Creates a standard error response.
     */
    public static ErrorResponse of(
            org.springframework.http.HttpStatus status,
            String message,
            String path,
            String errorCode) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .build();
    }
    
    /**
     * Debug information for error response (only in development mode).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebugInfo {
        private String exception;
        private String stackTrace;
    }
}
