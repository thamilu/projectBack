package com.eshop.app.user.infrastructure.keycloak;

import com.eshop.app.core.infrastructure.config.security.keycloak.CustomKeycloakJacksonProvider;
import com.eshop.app.core.infrastructure.config.security.keycloak.KeycloakConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Concrete implementation of {@link KeycloakAdminAdapter} that interacts directly with the standard
 * Keycloak Admin Client and manages client lifecycles.
 */
@Component
@Slf4j
public class KeycloakAdminAdapterImpl implements KeycloakAdminAdapter {

    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int READ_TIMEOUT_SECONDS = 15;
    private static final String NOT_INITIALIZED_MESSAGE =
            "Keycloak client has not been initialized or already destroyed";

    private final KeycloakConfig keycloakConfig;
    private volatile Keycloak keycloak;
    private volatile Client resteasyClient;

    public KeycloakAdminAdapterImpl(KeycloakConfig keycloakConfig) {
        this.keycloakConfig =
                Objects.requireNonNull(keycloakConfig, "keycloakConfig must not be null");
    }

    @PostConstruct
    public void init() {
        validateConfiguration();

        String safeServerUrl = sanitizeUrl(keycloakConfig.getAuthServerUrl());
        log.info(
                "Initializing Keycloak admin client realm=[{}] server=[{}]",
                keycloakConfig.getRealm(),
                safeServerUrl);

        this.resteasyClient =
                ClientBuilder.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .register(new CustomKeycloakJacksonProvider())
                        .build();

        this.keycloak =
                KeycloakBuilder.builder()
                        .serverUrl(keycloakConfig.getAuthServerUrl())
                        .realm(keycloakConfig.getRealm())
                        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                        .clientId(keycloakConfig.getClientId())
                        .clientSecret(keycloakConfig.getClientSecret())
                        .resteasyClient(resteasyClient)
                        .build();

        log.info("Keycloak admin client initialized successfully");
    }

    @PreDestroy
    public void destroy() {
        log.info("Shutting down Keycloak admin client");
        closeQuietly(keycloak, "Keycloak admin client");
        closeQuietly(resteasyClient, "RESTEasy client");
        // Null out references so post-shutdown calls fail fast with a clear
        // IllegalStateException instead of an obscure "client closed" error.
        keycloak = null;
        resteasyClient = null;
        log.info("Keycloak admin client shutdown complete");
    }

    @Override
    public List<RoleRepresentation> getUserRealmRoles(String userId) {
        requireNonBlank(userId, "userId");
        log.debug("Fetching effective realm roles for user: userId=[{}]", userId);
        return executeSafely(
                "getUserRealmRoles",
                userId,
                () -> getRealmResource().users().get(userId).roles().realmLevel().listEffective());
    }

    @Override
    public void addRealmRolesToUser(String userId, List<RoleRepresentation> roles) {
        requireNonBlank(userId, "userId");
        requireNonEmpty(roles, "roles");
        executeSafely(
                "addRealmRolesToUser",
                userId,
                () -> {
                    getRealmResource().users().get(userId).roles().realmLevel().add(roles);
                    return null;
                });
        log.info("Realm roles added to user: userId=[{}] roles={}", userId, roleNames(roles));
    }

    @Override
    public void removeRealmRolesFromUser(String userId, List<RoleRepresentation> roles) {
        requireNonBlank(userId, "userId");
        requireNonEmpty(roles, "roles");
        executeSafely(
                "removeRealmRolesFromUser",
                userId,
                () -> {
                    getRealmResource().users().get(userId).roles().realmLevel().remove(roles);
                    return null;
                });
        log.info("Realm roles removed from user: userId=[{}] roles={}", userId, roleNames(roles));
    }

    @Override
    public RoleRepresentation getRealmRole(String roleName) {
        requireNonBlank(roleName, "roleName");
        log.debug("Fetching realm role: roleName=[{}]", roleName);
        return executeSafely(
                "getRealmRole",
                roleName,
                () -> getRealmResource().roles().get(roleName).toRepresentation());
    }

    @Override
    public UserRepresentation getUserById(String userId) {
        requireNonBlank(userId, "userId");
        log.debug("Fetching user by id: userId=[{}]", userId);
        return executeSafely(
                "getUserById", userId, () -> getRealmResource().users().get(userId).toRepresentation());
    }

    @Override
    public void updateUser(String userId, UserRepresentation representation) {
        requireNonBlank(userId, "userId");
        requireNonNull(representation, "representation");
        executeSafely(
                "updateUser",
                userId,
                () -> {
                    getRealmResource().users().get(userId).update(representation);
                    return null;
                });
        log.info("User updated: userId=[{}]", userId);
    }

    @Override
    public List<UserRepresentation> searchUsersByEmail(String email, boolean exact) {
        requireNonBlank(email, "email");
        String maskedEmail = maskEmail(email);
        log.debug("Searching users by email: email=[{}] exact=[{}]", maskedEmail, exact);
        return executeSafely(
                "searchUsersByEmail",
                maskedEmail,
                () -> getRealmResource().users().searchByEmail(email, exact));
    }

    @Override
    public List<UserRepresentation> searchUsersByUsername(String username, boolean exact) {
        requireNonBlank(username, "username");
        log.debug("Searching users by username: username=[{}] exact=[{}]", username, exact);
        return executeSafely(
                "searchUsersByUsername",
                username,
                () -> getRealmResource().users().searchByUsername(username, exact));
    }

    private RealmResource getRealmResource() {
        Keycloak client = this.keycloak;
        if (client == null) {
            throw new IllegalStateException(NOT_INITIALIZED_MESSAGE);
        }
        return client.realm(keycloakConfig.getRealm());
    }

    /**
     * Executes a Keycloak admin operation, translating transport/HTTP-level failures
     * ({@link WebApplicationException}, {@link ProcessingException}) into a
     * {@link KeycloakAdminOperationException} so vendor exception types never leak
     * outside this adapter.
     */
    private <T> T executeSafely(String operation, String identifier, Supplier<T> action) {
        try {
            return action.get();
        } catch (WebApplicationException | ProcessingException e) {
            int status = (e instanceof WebApplicationException wae) ? wae.getResponse().getStatus() : -1;
            log.error(
                    "Keycloak admin operation failed: operation=[{}] identifier=[{}] status=[{}] reason=[{}]",
                    operation,
                    identifier,
                    status,
                    e.getMessage());
            throw new KeycloakAdminOperationException(
                    "Keycloak admin operation failed: " + operation, e);
        }
    }

    private void validateConfiguration() {
        requireConfigText(keycloakConfig.getAuthServerUrl(), "keycloak.auth-server-url");
        requireConfigText(keycloakConfig.getRealm(), "keycloak.realm");
        requireConfigText(keycloakConfig.getClientId(), "keycloak.client-id");
        requireConfigText(keycloakConfig.getClientSecret(), "keycloak.credentials.secret");

        if (CollectionUtils.isEmpty(keycloakConfig.getAllowedRoles())) {
            throw new IllegalStateException(
                    "keycloak.allowed-roles must contain at least one role");
        }
    }

    private void requireConfigText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Configuration property [" + propertyName + "] must not be blank");
        }
    }

    private static void requireNonBlank(String value, String paramName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
    }

    private static void requireNonNull(Object value, String paramName) {
        if (value == null) {
            throw new IllegalArgumentException(paramName + " must not be null");
        }
    }

    private static void requireNonEmpty(List<?> list, String paramName) {
        if (CollectionUtils.isEmpty(list)) {
            throw new IllegalArgumentException(paramName + " must not be empty");
        }
    }

    private static List<String> roleNames(List<RoleRepresentation> roles) {
        return roles.stream().map(RoleRepresentation::getName).toList();
    }

    /**
     * Masks an email address for logging purposes, retaining only the domain to avoid
     * writing PII in plaintext logs.
     */
    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        return at > 0 ? "***" + email.substring(at) : "***";
    }

    private String sanitizeUrl(String url) {
        if (!StringUtils.hasText(url)) return "[blank]";
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return "[malformed-url]";
            }
            return scheme + "://" + host + (uri.getPort() != -1 ? ":" + uri.getPort() : "");
        } catch (Exception e) {
            return "[invalid-url]";
        }
    }

    private void closeQuietly(AutoCloseable resource, String name) {
        if (resource != null) {
            try {
                resource.close();
                log.debug("{} closed successfully", name);
            } catch (Exception e) {
                log.warn("Error closing {}: {}", name, e.getMessage());
            }
        }
    }
}

