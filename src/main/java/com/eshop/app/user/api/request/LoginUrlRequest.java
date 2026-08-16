package com.eshop.app.user.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request parameters to generate OAuth2 authorization login URL")
public class LoginUrlRequest {

    @ValidRedirectUri
    @Size(max = 512, message = "Redirect URI too long")
    @Schema(
            description = "Optional redirect URI to redirect to after successful authentication",
            example = "https://example.com/callback",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String redirectUri;
}
