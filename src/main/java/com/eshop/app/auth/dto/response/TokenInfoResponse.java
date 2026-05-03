package com.eshop.app.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Token validation response with metadata.
 */
@Builder
@Schema(description = "Token validation result")
public record TokenInfoResponse(
    @Schema(description = "Whether token is valid")
    boolean valid,
    
    @Schema(description = "Username from token")
    String username,
    
    @Schema(description = "Token expiration time")
    Instant expiresAt,
    
    @Schema(description = "Token issued at time")
    Instant issuedAt,
    
    @Schema(description = "Granted authorities")
    List<String> authorities,
    
    @Schema(description = "Time until expiration in seconds")
    long expiresInSeconds
) {
    /**
     * Compact constructor with validation.
     */
    public TokenInfoResponse {
        Objects.requireNonNull(expiresAt, "expiresAt cannot be null");
        Objects.requireNonNull(issuedAt, "issuedAt cannot be null");
        authorities = authorities != null ? List.copyOf(authorities) : List.of();
    }
    
    /**
     * Constructor that computes expiresInSeconds automatically.
     */
    public TokenInfoResponse(boolean valid, String username, Instant expiresAt, 
                             Instant issuedAt, List<String> authorities) {
        this(valid, username, expiresAt, issuedAt, authorities,
             Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds()));
    }
}
