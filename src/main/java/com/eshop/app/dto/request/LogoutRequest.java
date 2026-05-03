package com.eshop.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Logout request")
public class LogoutRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
