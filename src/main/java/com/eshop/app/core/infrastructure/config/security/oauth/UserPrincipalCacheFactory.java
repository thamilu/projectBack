package com.eshop.app.core.infrastructure.config.security.oauth;

import com.eshop.app.dto.security.SyncStatus;
import com.eshop.app.dto.security.UserPrincipalCache;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * Factory for creating and updating UserPrincipalCache entries. Centralizes cache construction to
 * ensure DRY compliance.
 */
@Component
public class UserPrincipalCacheFactory {

    /**
     * Updates an existing cache entry or creates a new one from event data.
     *
     * @param existing The existing cache entry (may be null)
     * @param event The UserIdentitySyncEvent details
     * @param status The SyncStatus to set
     * @param userId The resolved database userId (null if pending/failed)
     * @return A new or updated UserPrincipalCache instance
     */
    public UserPrincipalCache buildOrUpdate(
            UserPrincipalCache existing,
            UserIdentitySyncEvent event,
            SyncStatus status,
            Long userId) {

        Instant now = Instant.now();

        if (existing != null) {
            UserPrincipalCache updated = existing.withSyncStatus(status).withLastSyncAttempt(now);

            if (userId != null) {
                updated = updated.withUserId(userId);
            }

            return updated;
        }

        return UserPrincipalCache.builder()
                .userId(userId) // Null is valid — no sentinel magic
                .email(resolveEmail(event))
                .roles(event.getRoles())
                .syncStatus(status)
                .lastSyncAttempt(now)
                .build();
    }

    private String resolveEmail(UserIdentitySyncEvent event) {
        String email = event.getEmail();
        return (email != null && !email.isBlank()) ? email : event.getKeycloakId();
    }
}
