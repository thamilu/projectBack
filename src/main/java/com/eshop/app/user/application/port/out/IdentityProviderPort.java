package com.eshop.app.user.application.port.out;

import com.eshop.app.user.application.dto.BulkOperationResult;
import java.util.List;

/** Identity provider port defining user role and status management contracts. */
public interface IdentityProviderPort {

    /**
     * Assigns a realm role to a user.
     *
     * @param userId The user identifier
     * @param roleName The role name
     */
    void assignRole(String userId, String roleName);

    /**
     * Assigns a realm role to a user identified by email.
     *
     * @param email The user email address
     * @param roleName The role name
     */
    void assignRoleByEmail(String email, String roleName);

    /**
     * Enables or disables a user account.
     *
     * @param userId The user identifier
     * @param enabled true to enable, false to disable
     */
    void setUserEnabled(String userId, boolean enabled);

    /**
     * Checks if a user account is enabled.
     *
     * @param userId The user identifier
     * @return true if enabled, false otherwise
     */
    boolean isUserEnabled(String userId);

    /**
     * Bulk enables or disables user accounts.
     *
     * @param keycloakIds List of user identifiers
     * @param enabled true to enable, false to disable
     * @return The bulk operation result DTO
     */
    BulkOperationResult bulkSetUserEnabled(List<String> keycloakIds, boolean enabled);

    /**
     * Revokes a realm role from a user.
     *
     * @param userId The user identifier
     * @param roleName The role name
     */
    void revokeRole(String userId, String roleName);
}
