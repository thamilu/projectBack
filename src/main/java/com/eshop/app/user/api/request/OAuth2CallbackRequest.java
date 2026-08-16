package com.eshop.app.user.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OAuth2CallbackRequest {

    @NotBlank(message = "Authorization code required")
    @Size(max = 2048, message = "Code too long")
    @Pattern(
            regexp = "^[A-Za-z0-9_\\-\\.\\~\\+\\/\\=]+$",
            message = "Invalid authorization code format")
    private String code;

    @NotBlank(message = "State required")
    @Size(max = 1024, message = "State too long")
    private String state;

    @ValidRedirectUri
    @Size(max = 512, message = "Redirect URI too long")
    private String redirectUri;
}
