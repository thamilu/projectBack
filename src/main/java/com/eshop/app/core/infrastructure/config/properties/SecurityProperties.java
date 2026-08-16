package com.eshop.app.core.infrastructure.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Hardened security configuration properties for Spring Boot 4.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    // NOTE: @Valid is required here for the @NotBlank constraints on Jwt/Admin below to
    // actually be enforced — without it, @Validated on the outer class does NOT cascade
    // into nested POJOs, so those constraints were previously declared but silently never
    // checked (a misconfigured/blank value would bind successfully with no startup error).
    @Valid
    private final Jwt jwt = new Jwt();
    @Valid
    private final Admin admin = new Admin();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String audience = "eshop-backend,eshop-client,account";

        private long clockSkewSeconds = 300;

        private boolean enforceEmailVerified = false;

        // Default false preserves existing lenient behavior (a missing 'aud' claim is
        // permitted) so enabling @Valid cascade above cannot change runtime auth outcomes
        // for any currently-working deployment. Set true once every Keycloak client in use
        // is confirmed to have an audience mapper configured (Keycloak omits 'aud' by
        // default unless one is added).
        private boolean requireAudience = false;

        public List<String> getAllowedAudiences() {
            return List.of(audience.split(","));
        }
    }

    @Getter
    @Setter
    public static class Admin {
        // No default: an admin-realm issuer URI is security-relevant and must be
        // explicitly configured. A "http://localhost:8080/realms/master" default here
        // would previously (before the @Valid fix above) let a misconfigured deployment
        // silently accept this obviously-wrong value instead of failing to start.
        @NotBlank
        private String issuerUri;
    }
}
