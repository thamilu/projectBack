package com.eshop.app.user.infrastructure.keycloak;

import com.eshop.app.user.application.dto.BulkOperationResult;
import com.eshop.app.user.application.port.out.IdentityProviderPort;
import com.eshop.app.user.application.service.KeycloakAtomicOperations;
import com.eshop.app.user.application.service.KeycloakBulkService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter implementing the {@link IdentityProviderPort} interface by delegating core identity
 * operations to {@link KeycloakAtomicOperations} and {@link KeycloakBulkService}.
 *
 * <p>This class performs no business logic of its own; it is a pure delegation layer between the
 * application port and the Keycloak-specific service collaborators. Any exception thrown by the
 * delegated collaborators (e.g., due to Keycloak connectivity or authorization failures) propagates
 * unchanged to the caller of this port.
 */
@Component
@RequiredArgsConstructor
public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

    @NonNull private final KeycloakAtomicOperations keycloakAtomicOperations;
    @NonNull private final KeycloakBulkService keycloakBulkService;

    @Override
    public void assignRole(String userId, String roleName) {
        keycloakAtomicOperations.assignRole(userId, roleName);
    }

    @Override
    public void assignRoleByEmail(String email, String roleName) {
        keycloakAtomicOperations.assignRoleByEmail(email, roleName);
    }

    @Override
    public void setUserEnabled(String userId, boolean enabled) {
        keycloakAtomicOperations.setUserEnabled(userId, enabled);
    }

    @Override
    public boolean isUserEnabled(String userId) {
        return keycloakAtomicOperations.isUserEnabled(userId);
    }

    @Override
    public BulkOperationResult bulkSetUserEnabled(List<String> keycloakIds, boolean enabled) {
        return keycloakBulkService.bulkSetUserEnabled(keycloakIds, enabled, keycloakAtomicOperations);
    }

    @Override
    public void revokeRole(String userId, String roleName) {
        keycloakAtomicOperations.revokeRole(userId, roleName);
    }
}

