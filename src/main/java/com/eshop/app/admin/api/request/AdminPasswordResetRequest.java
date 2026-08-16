package com.eshop.app.admin.api.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Admin-initiated password reset payload for a Keycloak-managed user.
 * Mirrors the platform password policy enforced in {@code ResetPasswordRequest}.
 */
@Data
@Schema(description = "Admin password reset request")
public class AdminPasswordResetRequest {

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be 8-100 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Password must contain uppercase, lowercase, digit, and special character")
    @Schema(description = "New password for the user", format = "password")
    private String password;

    @Schema(description = "Whether the user must change this password on next login", defaultValue = "false")
    private boolean temporary = false;
}
