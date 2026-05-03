package com.eshop.app.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Keycloak Configuration Properties
 *
 * Type-safe binding for properties under `keycloak.*`.
 * This class is validated at startup and exposes derived URIs used by security
 * components (issuer, token endpoint, JWKS URI, etc.).
 */
@Data
@Slf4j
@Validated
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfigProperties {

    // Optional: when not configured tests or environments that do not use Keycloak
    // will not fail. If these are provided, validateAndInit() enforces correctness.
    private String authServerUrl;

    private String realm;

    private String resource;

    private boolean publicClient = false;
    private boolean bearerOnly = true;

    @Valid
    @NestedConfigurationProperty
    @ToString.Exclude
    private Credentials credentials = new Credentials();

    @Valid
    @NestedConfigurationProperty
    private Ssl ssl = new Ssl();

    // Cached derived URIs
    private volatile String issuerUri;
    private volatile String jwksUri;
    private volatile String tokenUri;


    @PostConstruct
    public void validateAndInit() {
        // If Keycloak isn't configured (common in tests), skip strict validation and be silent.
        if (this.authServerUrl == null || this.realm == null || this.resource == null) {
            log.debug("Keycloak integration is disabled");
            return;
        }

        log.info("Validating Keycloak configuration for realm='{}'", realm);

        // Additional semantic checks when configured:
        if (!publicClient && (credentials == null || credentials.getSecret() == null || credentials.getSecret().isBlank())) {
            throw new IllegalStateException("Confidential clients must provide keycloak.credentials.secret or set keycloak.public-client=true");
        }

        if (publicClient && bearerOnly) {
            throw new IllegalStateException("Invalid configuration: public-client and bearer-only cannot both be true");
        }

        if (isProduction() && (ssl == null || ssl.getRequired() == Ssl.Required.NONE)) {
            throw new IllegalStateException("SSL must be enabled in production environments");
        }

        // Initialize derived URIs lazily on first access (compute them now for clarity)
        this.issuerUri = buildIssuerUri();
        this.jwksUri = this.issuerUri + "/protocol/openid-connect/certs";
        this.tokenUri = this.issuerUri + "/protocol/openid-connect/token";

        log.info("Keycloak issuer URI initialized: {}", issuerUri);
    }

    private boolean isProduction() {
        String profiles = System.getProperty("spring.profiles.active", "");
        return profiles.contains("prod") || profiles.contains("production");
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildIssuerUri() {
        return normalizeUrl(this.authServerUrl) + "/realms/" + this.realm;
    }

    // Derived URIs (cached)
    public String getIssuerUri() {
        String result = issuerUri;
        if (result == null) {
            synchronized (this) {
                if (issuerUri == null) {
                    issuerUri = buildIssuerUri();
                }
                result = issuerUri;
            }
        }
        return result;
    }

    public String getJwksUri() {
        String result = jwksUri;
        if (result == null) {
            synchronized (this) {
                if (jwksUri == null) jwksUri = getIssuerUri() + "/protocol/openid-connect/certs";
                result = jwksUri;
            }
        }
        return result;
    }

    public String getTokenUri() {
        String result = tokenUri;
        if (result == null) {
            synchronized (this) {
                if (tokenUri == null) tokenUri = getIssuerUri() + "/protocol/openid-connect/token";
                result = tokenUri;
            }
        }
        return result;
    }

    public String getAuthUrl() {
        return getIssuerUri() + "/protocol/openid-connect/auth";
    }

    public String getUserInfoUrl() {
        return getIssuerUri() + "/protocol/openid-connect/userinfo";
    }

    public String getLogoutUrl() {
        return getIssuerUri() + "/protocol/openid-connect/logout";
    }

    // Nested configuration classes
    @Data
    public static class Credentials {
        @ToString.Exclude
        private String secret;

        private String provider;

        public String getMaskedSecret() {
            if (secret == null || secret.length() < 8) return "***";
            return secret.substring(0, 4) + "****" + secret.substring(secret.length() - 4);
        }
    }

    @Data
    public static class Ssl {
        public enum Required { ALL, EXTERNAL, NONE }

        private Required required = Required.EXTERNAL;
        private String truststore;
        @ToString.Exclude
        private String truststorePassword;
        private boolean disableTrustManager = false;
    }

}