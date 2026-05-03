package com.eshop.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User login credentials")
public class LoginRequest {
    
    @NotBlank(message = "Username or email is required")
    @Schema(
        description = "Username or email address for login",
        example = "john_doe",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    @Schema(
        description = "User password",
        example = "SecurePass123!",
        requiredMode = Schema.RequiredMode.REQUIRED,
        format = "password"
    )
    private String password;
}
