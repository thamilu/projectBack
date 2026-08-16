package com.eshop.app.user.infrastructure.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import lombok.Data;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Binds and validates HTTP client timeout settings for authentication-related
 * operations, sourced from properties under the {@code auth.timeout} prefix.
 *
 * <p><strong>Validation invariant:</strong> every timeout must be strictly
 * positive (minimum 1 millisecond). A zero or negative duration is not a
 * valid client timeout and, depending on the underlying HTTP client, may be
 * silently treated as "no timeout" (infinite wait) instead of failing fast —
 * this class enforces the invariant explicitly via {@code @Validated}.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "auth.timeout")
@Validated
@Data
public class AuthTimeoutConfig {

    /** Timeout for the login/authentication call to the identity provider. */
    @NotNull
    @DurationMin(millis = 1)
    private Duration login = Duration.ofSeconds(5);

    /** Timeout for token refresh calls. */
    @NotNull
    @DurationMin(millis = 1)
    private Duration tokenRefresh = Duration.ofSeconds(3);

    /** Timeout for retrieving authenticated user info (userinfo endpoint). */
    @NotNull
    @DurationMin(millis = 1)
    private Duration userInfo = Duration.ofSeconds(2);

    /** Timeout for OAuth2/OIDC token introspection calls. */
    @NotNull
    @DurationMin(millis = 1)
    private Duration introspection = Duration.ofMillis(500);

    /** Timeout for OAuth2 authorization-code callback processing. */
    @NotNull
    @DurationMin(millis = 1)
    private Duration oauth2Callback = Duration.ofSeconds(8);

    /** Fallback timeout applied when no more specific timeout is configured. */
    @NotNull
    @DurationMin(millis = 1)
    private Duration defaultTimeout = Duration.ofSeconds(5);
}

