package com.eshop.app.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Modern principal representation for OIDC/Keycloak authenticated users.
 */
@Getter
@Builder
@AllArgsConstructor
public class PrincipalDetails {
    private final Long id;
    private final String username;
    private final String email;
    private final String keycloakId;

    public String getName() {
        return username != null ? username : email;
    }
}
