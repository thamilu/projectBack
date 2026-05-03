package com.eshop.app.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Keycloak public configuration response.
 * Contains only non-sensitive configuration for frontend.
 */
@Schema(description = "Keycloak public configuration")
public record ConfigResponse(
    @Schema(description = "Keycloak realm", example = "eshop")
    String realm,
    
    @Schema(description = "Keycloak auth URL", example = "https://auth.example.com")
    String authUrl,
    
    @Schema(description = "Client ID", example = "eshop-client")
    String clientId
) {
}
