package com.eshop.app.user.application.command;

import com.eshop.app.core.infrastructure.config.security.oauth.UserIdentitySyncEvent;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable command DTO representing user identity synchronization data. Resolves the long
 * parameter list code smell in synchronization services.
 */
@Value
@Builder
public class UserSyncCommand {
    String keycloakId;
    String email;
    String firstName;
    String lastName;
    String phone;
    boolean emailVerified;

    /**
     * Creates a UserSyncCommand from a UserIdentitySyncEvent.
     *
     * @param event The UserIdentitySyncEvent
     * @return UserSyncCommand DTO
     */
    public static UserSyncCommand from(UserIdentitySyncEvent event) {
        return UserSyncCommand.builder()
                .keycloakId(event.getKeycloakId())
                .email(event.getEmail())
                .firstName(event.getFirstName())
                .lastName(event.getLastName())
                .phone(event.getPhone())
                .emailVerified(Boolean.TRUE.equals(event.getEmailVerified()))
                .build();
    }
}
