package com.eshop.app.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;
import java.util.Objects;

/**
 * User information response from JWT token.
 * Thread-safe immutable DTO with null-safe builder pattern.
 */
@Builder
@Schema(description = "User information from JWT token")
public record UserInfoResponse(
    @Schema(description = "Username", example = "john.doe")
    String username,
    
    @Schema(description = "Email address", example = "john.doe@example.com")
    String email,
    
    @Schema(description = "First name", example = "John")
    String firstName,
    
    @Schema(description = "Last name", example = "Doe")
    String lastName,
    
    @Schema(description = "Full name", example = "John Doe")
    String fullName,
    
    @Schema(description = "User roles")
    List<String> roles,
    
    @Schema(description = "Whether email is verified")
    boolean emailVerified,
    
    @Schema(description = "Subject identifier (UUID)")
    String sub
) {
    /**
     * Compact constructor with validation and defensive copying.
     */
    public UserInfoResponse {
        username = username != null ? username : "unknown";
        roles = roles != null ? List.copyOf(roles) : List.of();
        Objects.requireNonNull(sub, "Subject (sub) cannot be null");
    }
}
