package com.eshop.app.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.eshop.app.config.KeycloakConfig;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.stereotype.Service;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import jakarta.annotation.PostConstruct;
import java.util.Collections;

/**
 * Service to interact with Keycloak Admin API.
 * Used for role management (assigning SELLER/DELIVERY_AGENT roles).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {

    private final KeycloakConfig keycloakConfig;
    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        // Initialize Keycloak Admin Client
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getAuthServerUrl())
                .realm(keycloakConfig.getRealm())
                .grantType("client_credentials")
                .clientId(keycloakConfig.getClientId())
                .clientSecret(keycloakConfig.getClientSecret())
                .build();
    }

    /**
     * Assign a realm role to a user.
     *
     * @param userId   The Keycloak User ID (UUID)
     * @param roleName The role to assign (e.g. "SELLER")
     */
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void assignRole(String userId, String roleName) {
        try {
            log.info("Assigning role '{}' to user '{}' in Keycloak", roleName, userId);
            String realm = keycloakConfig.getRealm();

            RealmResource realmResource = keycloak.realm(realm);
            UserResource userResource = realmResource.users().get(userId);

            // Verify user exists (optional, throws 404 if not found)
            // userResource.toRepresentation();

            // Get Role Representation
            RoleRepresentation role = realmResource.roles().get(roleName).toRepresentation();

            // Assign role
            userResource.roles().realmLevel().add(Collections.singletonList(role));

            log.info("Successfully assigned role '{}' to user '{}'", roleName, userId);

        } catch (jakarta.ws.rs.NotFoundException e) {
            log.warn("User or role '{}' not found in Keycloak for ID '{}'. This is common during local dev DB resets.",
                    roleName, userId);
            // We intentionally do not throw an exception here so JIT creation can proceed
        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user '{}'. Stack trace:", roleName, userId, e);
            throw new RuntimeException("Failed to assign role in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Assign a realm role to a user by username.
     * Helpful when we don't have the Keycloak ID yet.
     *
     * @param username The username to search for
     * @param roleName The role to assign
     */
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void assignRoleByUsername(String username, String roleName) {
        try {
            log.info("Assigning role '{}' to user '{}' (by username)", roleName, username);
            String realm = keycloakConfig.getRealm();

            // Search for user by username (exact match preferred)
            java.util.List<org.keycloak.representations.idm.UserRepresentation> users = keycloak.realm(realm).users()
                    .search(username, true);

            if (users == null || users.isEmpty()) {
                // Fallback to non-exact search if exact not supported by this version/config
                users = keycloak.realm(realm).users().search(username);
                if (users == null || users.isEmpty()) {
                    throw new RuntimeException("User not found in Keycloak with username: " + username);
                }
            }

            // Find the user with the matching username case-insensitively
            org.keycloak.representations.idm.UserRepresentation user = users.stream()
                    .filter(u -> u.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User found but username mismatch for: " + username));

            assignRole(user.getId(), roleName);

        } catch (Exception e) {
            log.error("Failed to assign role '{}' to username '{}'. Stack trace:", roleName, username, e);
            throw new RuntimeException("Failed to assign role by username: " + e.getMessage(), e);
        }
    }

    /**
     * Set user enabled/disabled status in Keycloak.
     *
     * @param userId  The Keycloak User ID (UUID)
     * @param enabled True to enable, false to disable
     */
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void setUserEnabled(String userId, boolean enabled) {
        try {
            log.info("Setting enabled={} for user '{}' in Keycloak", enabled, userId);
            RealmResource realmResource = keycloak.realm(keycloakConfig.getRealm());
            UserResource userResource = realmResource.users().get(userId);
            org.keycloak.representations.idm.UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);
            log.info("Successfully updated enabled status for user '{}'", userId);
        } catch (Exception e) {
            log.error("Failed to update enabled status for user '{}': {}", userId, e.getMessage());
            throw new RuntimeException("Failed to update user status in Keycloak", e);
        }
    }
}
