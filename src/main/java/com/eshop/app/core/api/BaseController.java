package com.eshop.app.core.api;

import com.eshop.app.core.security.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base controller with common utility methods for all controllers.
 *
 * <p>Provides JWT/Keycloak claim extraction helpers shared across controller
 * implementations in this application.
 */
@Slf4j
public abstract class BaseController {

    private static final String CLAIM_REALM_ACCESS = "realm_access";
    private static final String CLAIM_REALM_ACCESS_ROLES = "roles";
    private static final String CLAIM_EMAIL = "email";

    /**
     * Extract user ID from JWT token.
     *
     * @param jwt the authenticated JWT; may be {@code null}
     * @return the JWT subject, or {@link UserContext#ANONYMOUS_USER_ID} if {@code jwt} is
     *         {@code null}
     */
    protected String extractUserId(Jwt jwt) {
        return extractUserId(jwt, UserContext.ANONYMOUS_USER_ID);
    }

    /**
     * Extract user ID with a custom default value.
     *
     * @param jwt          the authenticated JWT; may be {@code null}
     * @param defaultValue value returned when {@code jwt} is {@code null}
     * @return the JWT subject, or {@code defaultValue} if {@code jwt} is {@code null}
     */
    protected String extractUserId(Jwt jwt, String defaultValue) {
        return jwt != null ? jwt.getSubject() : defaultValue;
    }

    /**
     * Extract user context with all claims.
     *
     * @param jwt the authenticated JWT; may be {@code null}
     * @return a populated {@link UserContext}, or {@link UserContext#anonymous()} if
     *         {@code jwt} is {@code null}
     */
    protected UserContext extractUserContext(Jwt jwt) {
        if (jwt == null) {
            return UserContext.anonymous();
        }

        return UserContext.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString(CLAIM_EMAIL))
                .roles(extractRoles(jwt))
                .build();
    }

    /**
     * Extract roles from the Keycloak {@code realm_access.roles} claim structure.
     *
     * <p>Defensively validates the claim's runtime shape since JWT claims originate from an
     * external identity provider and are not guaranteed to match the expected structure
     * (e.g., a misconfigured protocol/claim mapper). Malformed structures are logged and
     * treated as "no roles" rather than propagating a {@link ClassCastException}.
     *
     * @param jwt the authenticated JWT; may be {@code null}
     * @return an unmodifiable set of role names; empty if {@code jwt} is {@code null} or the
     *         claim is absent/malformed
     */
    protected Set<String> extractRoles(Jwt jwt) {
        if (jwt == null) {
            return Set.of();
        }

        Object realmAccessClaim = jwt.getClaim(CLAIM_REALM_ACCESS);
        if (!(realmAccessClaim instanceof Map<?, ?> realmAccess)) {
            return Set.of();
        }

        Object rolesClaim = realmAccess.get(CLAIM_REALM_ACCESS_ROLES);
        if (!(rolesClaim instanceof List<?> rawRoles)) {
            return Set.of();
        }

        Set<String> roles = new HashSet<>();
        for (Object role : rawRoles) {
            if (role instanceof String roleName) {
                roles.add(roleName);
            } else if (role != null) {
                log.warn(
                        "Ignoring non-string role entry of type [{}] in {}.{} claim",
                        role.getClass().getName(),
                        CLAIM_REALM_ACCESS,
                        CLAIM_REALM_ACCESS_ROLES);
            }
        }
        return Collections.unmodifiableSet(roles);
    }
}
