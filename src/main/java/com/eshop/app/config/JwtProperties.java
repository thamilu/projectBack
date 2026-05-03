package com.eshop.app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.Set;

/**
 * JWT security configuration properties.
 * <p>
 * Binds to properties prefixed with {@code app.security.jwt}.
 * All security-critical properties are validated at startup.
 * </p>
 */
@ConfigurationProperties(prefix = "app.security.jwt")
@Validated
public record JwtProperties(

        @DefaultValue("ROLE_")
        @NotBlank(message = "authorityPrefix must not be blank")
        String authorityPrefix,

        @DefaultValue("roles")
        @NotBlank(message = "authoritiesClaimName must not be blank")
        String authoritiesClaimName,

        @NotEmpty(message = "At least one audience must be configured")
        Set<@NotBlank String> audiences,

        @NotBlank(message = "Issuer URI is required for token validation")
        String issuerUri,

        @NotBlank(message = "JWK Set URI is required for signature verification")
        String jwkSetUri,

        @NotEmpty(message = "At least one allowed algorithm must be specified")
        @DefaultValue("RS256")
        Set<@NotBlank String> allowedAlgorithms,

        @NotNull
        @DefaultValue("PT15M")
        Duration accessTokenExpiry,

        @NotNull
        @DefaultValue("PT30S")
        Duration clockSkew,

        @Valid
        @NotNull
        ClaimMappings claimMappings

) {

    public JwtProperties {
        // Security validation - prevent insecure algorithms
        if (allowedAlgorithms != null) {
            for (String alg : allowedAlgorithms) {
                if (alg == null) continue;
                String lower = alg.trim().toLowerCase();
                if ("none".equals(lower) || "hs256".equals(lower)) {
                    throw new SecurityException("Insecure algorithm '" + alg + "' is not permitted");
                }
            }
        }

        // Enforce HTTPS issuer in non-local environments (allow localhost)
        if (issuerUri != null && !issuerUri.isBlank()) {
            String lower = issuerUri.toLowerCase();
            if (!lower.startsWith("https://") && !lower.contains("localhost")) {
                throw new SecurityException("Issuer URI must use HTTPS in production: " + issuerUri);
            }
        }
    }

    public boolean isValidAudience(String audience) {
        return audiences != null && audiences.contains(audience);
    }

    public record ClaimMappings(
            @DefaultValue("preferred_username")
            @NotBlank
            String usernameClaim,

            @DefaultValue("email")
            @NotBlank
            String emailClaim,

            @DefaultValue("realm_access.roles")
            @NotBlank
            String realmRolesPath
    ) {
        public ClaimMappings {
            // basic validation
            if (usernameClaim != null && usernameClaim.isBlank()) {
                throw new IllegalArgumentException("usernameClaim cannot be blank");
            }
        }
    }

}



