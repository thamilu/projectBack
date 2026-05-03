package com.eshop.app.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Logout URL response.
 */
@Schema(description = "Keycloak logout URL")
public record LogoutUrlResponse(
    @Schema(description = "Complete logout URL with validated redirect", 
            example = "https://auth.example.com/realms/eshop/protocol/openid-connect/logout")
    String logoutUrl
) {
}
