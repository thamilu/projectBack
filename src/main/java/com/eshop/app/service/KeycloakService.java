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
        // [HARDEN] Build a custom ResteasyClient to handle Keycloak schema changes gracefully
        // We register CustomKeycloakJacksonProvider to ignore unknown properties (like "multivalued")
        jakarta.ws.rs.client.Client resteasyClient = jakarta.ws.rs.client.ClientBuilder.newClient()
                .register(new com.eshop.app.config.CustomKeycloakJacksonProvider());

        // Initialize Keycloak Admin Client with custom resteasyClient
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakConfig.getAuthServerUrl())
                .realm(keycloakConfig.getRealm())
                .grantType("client_credentials")
                .clientId(keycloakConfig.getClientId())
                .clientSecret(keycloakConfig.getClientSecret())
                .resteasyClient(resteasyClient)
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

        } catch (Exception e) {
            log.error("Failed to assign role '{}' to user '{}'. Reason: {}", roleName, userId, e.getMessage());
            throw new RuntimeException("Failed to assign role in Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Assign a realm role to a user by email.
     * Helpful when we don't have the Keycloak ID yet.
     *
     * @param email The email (which is the username in Keycloak) to search for
     * @param roleName The role to assign
     */
    @Retry(name = "keycloak")
    @CircuitBreaker(name = "keycloak")
    public void assignRoleByEmail(String email, String roleName) {
        try {
            log.info("Assigning role '{}' to user with email '{}' (by search)", roleName, email);
            String realm = keycloakConfig.getRealm();

            // Search for user by email (as username)
            java.util.List<org.keycloak.representations.idm.UserRepresentation> users = keycloak.realm(realm).users()
                    .search(email, true);

            if (users == null || users.isEmpty()) {
                // Fallback to non-exact search
                users = keycloak.realm(realm).users().search(email);
                if (users == null || users.isEmpty()) {
                    throw new RuntimeException("User not found in Keycloak with email: " + email);
                }
            }

            // Find the user with matching email or username
            org.keycloak.representations.idm.UserRepresentation user = users.stream()
                    .filter(u -> email.equalsIgnoreCase(u.getEmail()) || email.equalsIgnoreCase(u.getUsername()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User found but identity mismatch for: " + email));

            assignRole(user.getId(), roleName);

        } catch (Exception e) {
            log.error("Failed to assign role '{}' to email '{}'. Stack trace:", roleName, email, e);
            throw new RuntimeException("Failed to assign role by email: " + e.getMessage(), e);
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
