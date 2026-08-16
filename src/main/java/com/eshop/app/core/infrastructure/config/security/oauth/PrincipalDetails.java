package com.eshop.app.core.infrastructure.config.security.oauth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Modern principal representation for OIDC/Keycloak authenticated users.
 */
@Getter
@Builder
@AllArgsConstructor
public class PrincipalDetails implements java.security.Principal {
    private final Long id;
    private final String email;
    private final String keycloakId;

    public String getName() {
        return email;
    }

    public boolean hasResolvedId() {
        return id != null && id > 0;
    }
}
