package com.eshop.app.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Immutable Keycloak configuration bound from properties (constructor binding).
 * Sensitive fields (client secret, admin credentials) are intentionally
 * package-private to avoid accidental public exposure.
 */
@ConfigurationProperties(prefix = "keycloak")
@Validated
public class KeycloakConfig {

    private static final Logger log = LoggerFactory.getLogger(KeycloakConfig.class);

    @NotBlank
    private final String authServerUrl;

    @NotBlank
    private final String realm;

    @NotBlank
    private final String clientId;

    @NotBlank
    private final String clientSecret; // no public getter

    private final Admin admin; // no public getters for admin credentials

    // cached endpoints
    private String tokenEndpoint;
    private String authorizationEndpoint;
    private String userInfoEndpoint;
    private String logoutEndpoint;
    private String introspectEndpoint;
    private String revokeEndpoint;
    private String certsEndpoint;
    private String wellKnownEndpoint;
    private String adminUsersEndpoint;

    public KeycloakConfig(String authServerUrl,
                          String realm,
                          String clientId,
                          String clientSecret,
                          Admin admin) {
        this.authServerUrl = normalizeUrl(authServerUrl);
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.admin = admin;
    }

    @PostConstruct
    void initializeEndpoints() {
        String baseRealmUrl = authServerUrl + "/realms/" + realm;
        String oidcBase = baseRealmUrl + "/protocol/openid-connect";

        this.tokenEndpoint = oidcBase + "/token";
        this.authorizationEndpoint = oidcBase + "/auth";
        this.userInfoEndpoint = oidcBase + "/userinfo";
        this.logoutEndpoint = oidcBase + "/logout";
        this.introspectEndpoint = oidcBase + "/token/introspect";
        this.revokeEndpoint = oidcBase + "/revoke";
        this.certsEndpoint = oidcBase + "/certs";
        this.wellKnownEndpoint = baseRealmUrl + "/.well-known/openid-configuration";
        this.adminUsersEndpoint = authServerUrl + "/admin/realms/" + realm + "/users";

        log.debug("Keycloak endpoints initialized for realm: {}", realm);
    }

    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Auth server URL cannot be blank");
        }
        String normalized = url.trim();
        // Validate
        try {
            new java.net.URI(normalized).toURL();
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid auth server URL: " + url, e);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    // Public (non-sensitive) getters
    public String getAuthServerUrl() { return authServerUrl; }
    public String getRealm() { return realm; }
    public String getClientId() { return clientId; }

    // Endpoint getters (O(1), cached)
    public String getTokenEndpoint() { return tokenEndpoint; }
    public String getAuthorizationEndpoint() { return authorizationEndpoint; }
    public String getUserInfoEndpoint() { return userInfoEndpoint; }
    public String getLogoutEndpoint() { return logoutEndpoint; }
    public String getIntrospectEndpoint() { return introspectEndpoint; }
    public String getRevokeEndpoint() { return revokeEndpoint; }
    public String getCertsEndpoint() { return certsEndpoint; }
    public String getWellKnownEndpoint() { return wellKnownEndpoint; }
    public String getAdminUsersEndpoint() { return adminUsersEndpoint; }

    // Dynamic endpoints (require parameter)
    public String getAdminUserEndpoint(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("User ID cannot be null or blank");
        return adminUsersEndpoint + "/" + userId;
    }

    public String getAdminResetPasswordEndpoint(String userId) {
        return getAdminUserEndpoint(userId) + "/reset-password";
    }

    // Sensitive accessors - intentionally minimal but public for services that need them
    public String getClientSecret() { return clientSecret; }
    public String getAdminClientId() { return admin == null ? null : admin.getClientId(); }
    public String getAdminUsername() { return admin == null ? null : admin.getUsername(); }
    public String getAdminPassword() { return admin == null ? null : admin.getPassword(); }

    @Override
    public String toString() {
        return "KeycloakConfig{" +
            "authServerUrl='" + authServerUrl + '\'' +
            ", realm='" + realm + '\'' +
            ", clientId='" + clientId + '\'' +
            ", clientSecret='***REDACTED***'" +
            ", admin=" + (admin == null ? "null" : "***REDACTED***") +
            "}";
    }

    public static final class Admin {
        private final String username;
        private final String password;
        private final String clientId;

        public Admin(String username, String password, String clientId) {
            this.username = username;
            this.password = password;
            this.clientId = clientId;
        }

        // package-private access only
        String getUsername() { return username; }
        String getPassword() { return password; }
        String getClientId() { return clientId; }

        @Override
        public String toString() {
            return "Admin{username='" + username + "', password='***REDACTED***', clientId='" + clientId + "'}";
        }
    }
}
