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
    /**
     * The verified JWT's {@code iss} claim, copied here at authentication time.
     *
     * <p>Do NOT put the raw {@link org.springframework.security.oauth2.jwt.Jwt} (or any
     * other value needed by an authorization decision) into an
     * {@code Authentication}'s <em>credentials</em>. {@code ProviderManager} erases
     * credentials after successful authentication by default
     * ({@code eraseCredentialsAfterAuthentication=true}), which runs before the
     * authorization filter/{@code @PreAuthorize} phase — any {@code AuthorizationManager}
     * reading {@code auth.getCredentials()} sees {@code null}, always, regardless of what
     * the converter put there. The <em>principal</em> is never erased, so that's the only
     * safe place to carry data an authorization check needs to read later.</p>
     */
    private final String issuer;

    // Commonly-needed profile claims, copied at authentication time for the same reason
    // `issuer` is: code downstream of the authorization phase cannot recover them from
    // `auth.getCredentials()` (erased) — see the `issuer` javadoc above.
    private final String givenName;
    private final String familyName;
    private final String phoneNumber;
    private final Boolean emailVerified;

    public String getName() {
        return email;
    }

    public boolean hasResolvedId() {
        return id != null && id > 0;
    }
}
