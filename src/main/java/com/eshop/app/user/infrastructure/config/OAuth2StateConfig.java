package com.eshop.app.user.infrastructure.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties controlling OAuth2 "state" parameter generation
 * and verification, used as CSRF protection during the OAuth2 authorization
 * code flow.
 *
 * <p><b>Security note:</b> {@code signingKey} is a cryptographic secret.
 * It is excluded from {@link #toString()} and {@code equals()/hashCode()}
 * to prevent accidental disclosure via logs, exception messages, or
 * debugging/APM tooling. Do not log this bean or the {@code signingKey}
 * field anywhere else in the application.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.oauth2.state")
@Validated
@Data
public class OAuth2StateConfig {

    private static final int MIN_SIGNING_KEY_BYTES = 32; // 256 bits

    @NotNull
    private Duration validity = Duration.ofMinutes(5);

    @NotNull
    private Duration clockSkewTolerance = Duration.ofMinutes(1);

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    private String signingKey;

    /**
     * Validates security-sensitive properties at application startup so
     * misconfiguration fails fast instead of silently weakening OAuth2
     * state (CSRF) protection.
     */
    @PostConstruct
    public void validate() {
        int signingKeyBytes =
                signingKey == null ? 0 : signingKey.getBytes(StandardCharsets.UTF_8).length;
        if (signingKeyBytes < MIN_SIGNING_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.security.oauth2.state.signing-key (env: OAUTH2_STATE_SIGNING_KEY) "
                            + "must be set and at least 256 bits (32 bytes UTF-8)");
        }
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalStateException(
                    "app.security.oauth2.state.validity must be a positive duration");
        }
        if (clockSkewTolerance == null || clockSkewTolerance.isNegative()) {
            throw new IllegalStateException(
                    "app.security.oauth2.state.clock-skew-tolerance must not be negative");
        }
    }
}

