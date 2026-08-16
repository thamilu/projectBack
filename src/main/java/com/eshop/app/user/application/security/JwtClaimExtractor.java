package com.eshop.app.user.application.security;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Extracts a user's effective roles from a validated (signature-verified) {@link Jwt}
 * principal issued by Keycloak.
 *
 * <p><strong>CANONICAL, SINGLE-SOURCE-OF-TRUTH IMPLEMENTATION:</strong> this is the one
 * place in the codebase that knows how to read Keycloak role claims, used both by
 * {@code SecurityConfig#jwtAuthenticationConverter()} (which builds the real
 * {@code GrantedAuthority} set that drives every {@code @PreAuthorize} decision in the
 * application) and by diagnostic endpoints ({@code SessionController}, {@code MeController})
 * that report a user's roles back to the client. Both consumers see an identical role
 * set by construction, not by convention — there is no second implementation to drift
 * out of sync. Any new code needing a user's roles from a {@link Jwt} MUST use
 * {@link #extractEffectiveRoles} rather than re-implementing claim navigation.</p>
 *
 * <p>Merges roles from every source Keycloak may populate, matching this application's
 * configured claim paths ({@link AppProperties.Security}):</p>
 * <ol>
 *   <li>Realm roles, at the configured path (default {@code realm_access.roles})</li>
 *   <li>Client-scoped roles, under {@code resource_access.<client-id>.<rolesKey>} for
 *       every client present in the token</li>
 *   <li>A root-level {@code roles} claim (simplified/alternative token structure)</li>
 *   <li>A {@code groups} claim (commonly used in OIDC for organizational roles)</li>
 * </ol>
 *
 * <p>Unlike the now-removed {@code JwtTokenExtractor} (a separate, unrelated utility
 * that parsed raw, unverified token strings — deleted from this codebase as an unused,
 * higher-risk parallel path), this class operates exclusively on an already-verified
 * {@link Jwt} object provided by Spring Security's {@code JwtDecoder} pipeline: every
 * claim accessed here has already passed signature/expiry validation.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtClaimExtractor {

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";
    private static final String ROOT_ROLES_CLAIM = "roles";
    private static final String GROUPS_CLAIM = "groups";

    private final AppProperties appProperties;

    /**
     * Extracts the union of all roles Keycloak has assigned to this user, across every
     * source this application recognizes (realm, client-scoped, root-level, groups).
     *
     * @param jwt the verified JWT principal; must not be null
     * @return the deduplicated, insertion-ordered list of role names, or an empty list
     *         if {@code jwt} is null or no roles are present in any recognized claim
     */
    public List<String> extractEffectiveRoles(Jwt jwt) {
        if (jwt == null) {
            log.warn("extractEffectiveRoles called with a null Jwt; returning empty role list.");
            return List.of();
        }
        try {
            AppProperties.Security sec = appProperties.getSecurity();
            Set<String> roles = new LinkedHashSet<>();

            // 1. Realm roles at the configured claim path (e.g. "realm_access.roles").
            addStrings(roles, resolveClaimPath(jwt, sec.getClaimRealms(), sec.getClaimRoles()));

            // 2. Client-scoped roles: resource_access.<any-client>.<rolesKey>, merged
            //    across every client present in the token.
            Object resourceAccessObj = jwt.getClaim(RESOURCE_ACCESS_CLAIM);
            if (resourceAccessObj instanceof Map<?, ?> resourceAccess) {
                for (Object resource : resourceAccess.values()) {
                    if (resource instanceof Map<?, ?> resourceMap) {
                        addStrings(roles, resourceMap.get(sec.getClaimRoles()));
                    }
                }
            }

            // 3. Root-level 'roles' claim (alternative/simplified token structure).
            addStrings(roles, jwt.getClaim(ROOT_ROLES_CLAIM));

            // 4. 'groups' claim (commonly used in OIDC for organizational roles).
            addStrings(roles, jwt.getClaim(GROUPS_CLAIM));

            return List.copyOf(roles);
        } catch (Exception e) {
            log.warn("Failed to extract roles from JWT sub={}", jwt.getSubject(), e);
            return List.of();
        }
    }

    /**
     * Resolves a (possibly dotted, e.g. {@code "realm_access.roles"}) claim path down
     * to its final value. A single-segment path (e.g. {@code "realm_access"}) is
     * resolved as {@code jwt.getClaim(path).get(rolesKey)}.
     */
    @SuppressWarnings("unchecked")
    private Object resolveClaimPath(Jwt jwt, String claimPath, String rolesKey) {
        if (claimPath.contains(".")) {
            String[] parts = claimPath.split("\\.");
            Map<String, Object> current = jwt.getClaims();
            for (int i = 0; i < parts.length - 1 && current != null; i++) {
                Object next = current.get(parts[i]);
                current = (next instanceof Map) ? (Map<String, Object>) next : null;
            }
            return current != null ? current.get(parts[parts.length - 1]) : null;
        }
        Object claim = jwt.getClaim(claimPath);
        return (claim instanceof Map<?, ?> claimMap) ? claimMap.get(rolesKey) : null;
    }

    /**
     * Defensively adds every {@code String} element of {@code value} (when it is a
     * {@code List}) to {@code target}, silently skipping non-string entries — a
     * malformed/unexpected claim value degrades this user's role list rather than
     * failing the whole extraction.
     */
    private void addStrings(Set<String> target, Object value) {
        if (value instanceof List<?> list) {
            list.stream().filter(String.class::isInstance).map(String.class::cast).forEach(target::add);
        }
    }
}
