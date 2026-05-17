package com.eshop.app.user.application.port.in;

import java.util.Collection;

/**
 * Use case for synchronizing local user data with Identity Provider (Keycloak).
 */
public interface IdentitySyncUseCase {
    Long createUserFromKeycloak(String keycloakId, String firstName, String lastName, String phoneNumber);
    Long syncUserFromKeycloak(String keycloakId, String email, String firstName, String lastName, String phoneNumber, Boolean emailVerified);
    void syncKeycloakId(Long userId, String keycloakId);
    void syncUserRoles(Long userId, Collection<String> keycloakRoles);
}
