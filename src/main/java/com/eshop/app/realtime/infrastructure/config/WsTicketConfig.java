package com.eshop.app.realtime.infrastructure.config;

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
 * Configuration properties controlling WebSocket connection ticket generation
 * and verification.
 *
 * <p>A ticket is a short-lived, single-use credential minted server-side (see
 * {@code WsTicketController}) so a browser can authenticate a WebSocket
 * handshake without ever holding the user's real, long-lived Keycloak access
 * token — which would otherwise be a live credential valid against any
 * backend REST endpoint, not just this one connection.
 *
 * <p><b>Security note:</b> {@code signingKey} is a cryptographic secret,
 * deliberately independent from {@code app.security.oauth2.state.signing-key}
 * (see {@code OAuth2StateConfig}) — the two protect different things with
 * different threat models and lifetimes (OAuth2 state protects a redirect-flow
 * CSRF token measured in minutes during login; this protects a live
 * connection credential minted continuously during a session). Sharing a key
 * would mean rotating one for an incident forces rotating the other. It is
 * excluded from {@link #toString()} and {@code equals()/hashCode()} to
 * prevent accidental disclosure via logs, exception messages, or debugging/APM
 * tooling. Do not log this bean or the {@code signingKey} field anywhere else.
 */
@Configuration
@ConfigurationProperties(prefix = "app.realtime.ticket")
@Validated
@Data
public class WsTicketConfig {

    private static final int MIN_SIGNING_KEY_BYTES = 32; // 256 bits

    @NotNull
    private Duration validity = Duration.ofSeconds(45);

    @NotNull
    private Duration clockSkewTolerance = Duration.ofSeconds(5);

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotBlank
    private String signingKey;

    /**
     * Validates security-sensitive properties at application startup so
     * misconfiguration fails fast instead of silently weakening WebSocket
     * handshake authentication.
     */
    @PostConstruct
    public void validate() {
        int signingKeyBytes =
                signingKey == null ? 0 : signingKey.getBytes(StandardCharsets.UTF_8).length;
        if (signingKeyBytes < MIN_SIGNING_KEY_BYTES) {
            throw new IllegalStateException(
                    "app.realtime.ticket.signing-key (env: WS_TICKET_SIGNING_KEY) "
                            + "must be set and at least 256 bits (32 bytes UTF-8)");
        }
        if (validity == null || validity.isZero() || validity.isNegative()) {
            throw new IllegalStateException("app.realtime.ticket.validity must be a positive duration");
        }
        if (clockSkewTolerance == null || clockSkewTolerance.isNegative()) {
            throw new IllegalStateException(
                    "app.realtime.ticket.clock-skew-tolerance must not be negative");
        }
    }
}
