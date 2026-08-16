package com.eshop.app.user.infrastructure.keycloak;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Narrow adapter interface for direct Keycloak Admin Client operations.
 *
 * <p><b>Exception contract:</b> all methods in this interface delegate to remote Keycloak Admin
 * REST API calls. Implementations are expected to propagate communication failures (network
 * errors, timeouts, non-2xx Admin API responses) as unchecked runtime exceptions; callers should
 * not assume any operation here is guaranteed to succeed without error handling.
 */
public interface KeycloakAdminAdapter {

    /**
     * Gets the effective realm-level roles assigned to a user.
     *
     * @param userId The user UUID
     * @return List of assigned role representations; an empty list if the user has no realm roles
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    List<RoleRepresentation> getUserRealmRoles(String userId);

    /**
     * Assigns realm-level roles to a user.
     *
     * @param userId The user UUID
     * @param roles The roles to assign; implementations should treat a {@code null} or empty list
     *     as a no-op
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    void addRealmRolesToUser(String userId, List<RoleRepresentation> roles);

    /**
     * Revokes realm-level roles from a user.
     *
     * @param userId The user UUID
     * @param roles The roles to revoke; implementations should treat a {@code null} or empty list
     *     as a no-op
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    void removeRealmRolesFromUser(String userId, List<RoleRepresentation> roles);

    /**
     * Fetches a role representation by name.
     *
     * @param roleName The role name
     * @return The role representation, or {@code null} if no role with this name exists in the
     *     realm
     * @throws RuntimeException if the underlying Keycloak Admin API call fails for a reason other
     *     than the role not being found
     */
    @Nullable
    RoleRepresentation getRealmRole(String roleName);

    /**
     * Fetches a user representation by ID.
     *
     * @param userId The user UUID
     * @return The user representation, or {@code null} if no user with this ID exists
     * @throws RuntimeException if the underlying Keycloak Admin API call fails for a reason other
     *     than the user not being found
     */
    @Nullable
    UserRepresentation getUserById(String userId);

    /**
     * Updates user representation details in Keycloak.
     *
     * @param userId The user UUID
     * @param representation The updated user representation
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    void updateUser(String userId, UserRepresentation representation);

    /**
     * Searches users by email address.
     *
     * <p><b>Result size caveat:</b> when {@code exact} is {@code false}, this performs a partial
     * match search which may return a large result set on realms with many users. Implementations
     * should apply the Keycloak Admin Client's own pagination (first-result/max-results) to bound
     * the response size; this interface does not currently expose pagination parameters to
     * callers.
     *
     * @param email The email to search
     * @param exact true for exact match, false for partial search
     * @return List of matching user representations; an empty list if none match
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    List<UserRepresentation> searchUsersByEmail(String email, boolean exact);

    /**
     * Searches users by username.
     *
     * <p><b>Result size caveat:</b> when {@code exact} is {@code false}, this performs a partial
     * match search which may return a large result set on realms with many users. Implementations
     * should apply the Keycloak Admin Client's own pagination (first-result/max-results) to bound
     * the response size; this interface does not currently expose pagination parameters to
     * callers.
     *
     * @param username The username to search
     * @param exact true for exact match, false for partial search
     * @return List of matching user representations; an empty list if none match
     * @throws RuntimeException if the underlying Keycloak Admin API call fails
     */
    List<UserRepresentation> searchUsersByUsername(String username, boolean exact);
}

