package com.eshop.app.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Authentication service health response.
 */
@Schema(description = "Authentication service health status")
public record HealthResponse(
    @Schema(description = "Health status", example = "UP")
    String status,
    
    @Schema(description = "Health check timestamp")
    Instant timestamp
) {
}
