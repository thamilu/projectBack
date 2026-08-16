package com.eshop.app.user.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Verifies the canonical role-extraction logic shared by {@code SecurityConfig}'s real
 * {@code GrantedAuthority}-building converter and the diagnostic endpoints
 * ({@code SessionController}, {@code MeController}) that report a user's roles back to
 * the client. This is the single most security-relevant test in this area of the
 * codebase: a regression here silently changes real authorization behavior.
 */
class JwtClaimExtractorTest {

    private JwtClaimExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new JwtClaimExtractor(new AppProperties()); // default: claimRealms="realm_access", claimRoles="roles"
    }

    private Jwt jwtWithClaims(Map<String, Object> claims) {
        Map<String, Object> allClaims = new HashMap<>(claims);
        allClaims.putIfAbsent("sub", "user-123");
        Instant now = Instant.now();
        Map<String, Object> headers = Map.of("alg", "none");
        return new Jwt("token-value", now, now.plusSeconds(3600), headers, allClaims);
    }

    @Test
    void extractEffectiveRoles_nullJwt_returnsEmptyListWithoutThrowing() {
        assertThat(extractor.extractEffectiveRoles(null)).isEmpty();
    }

    @Test
    void extractEffectiveRoles_noRoleClaims_returnsEmptyList() {
        Jwt jwt = jwtWithClaims(Map.of("email", "user@example.com"));

        assertThat(extractor.extractEffectiveRoles(jwt)).isEmpty();
    }

    @Test
    void extractEffectiveRoles_realmRolesOnly_extracted() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("CUSTOMER", "SELLER"));
        Jwt jwt = jwtWithClaims(Map.of("realm_access", realmAccess));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactlyInAnyOrder("CUSTOMER", "SELLER");
    }

    @Test
    void extractEffectiveRoles_clientScopedRolesOnly_extractedAcrossAllClients() {
        Map<String, Object> clientA = Map.of("roles", List.of("ADMIN"));
        Map<String, Object> clientB = Map.of("roles", List.of("SUPPORT"));
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("eshop-frontend", clientA);
        resourceAccess.put("eshop-admin-console", clientB);
        Jwt jwt = jwtWithClaims(Map.of("resource_access", resourceAccess));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactlyInAnyOrder("ADMIN", "SUPPORT");
    }

    @Test
    void extractEffectiveRoles_realmAndClientRoles_mergedAndDeduplicated() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("CUSTOMER"));
        Map<String, Object> clientRoles = Map.of("roles", List.of("CUSTOMER", "SELLER"));
        Map<String, Object> resourceAccess = Map.of("eshop-client", clientRoles);
        Jwt jwt = jwtWithClaims(Map.of("realm_access", realmAccess, "resource_access", resourceAccess));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactlyInAnyOrder("CUSTOMER", "SELLER");
    }

    @Test
    void extractEffectiveRoles_rootLevelRolesClaim_extracted() {
        Jwt jwt = jwtWithClaims(Map.of("roles", List.of("DELIVERY_AGENT")));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactly("DELIVERY_AGENT");
    }

    @Test
    void extractEffectiveRoles_groupsClaim_extracted() {
        Jwt jwt = jwtWithClaims(Map.of("groups", List.of("warehouse-team")));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactly("warehouse-team");
    }

    @Test
    void extractEffectiveRoles_allFourSources_allMergedAndDeduplicated() {
        Map<String, Object> realmAccess = Map.of("roles", List.of("CUSTOMER"));
        Map<String, Object> resourceAccess = Map.of("client-x", Map.of("roles", List.of("ADMIN")));
        Map<String, Object> claims = new HashMap<>();
        claims.put("realm_access", realmAccess);
        claims.put("resource_access", resourceAccess);
        claims.put("roles", List.of("CUSTOMER", "ROOT_ROLE")); // CUSTOMER overlaps with realm — must dedupe
        claims.put("groups", List.of("ops-team"));
        Jwt jwt = jwtWithClaims(claims);

        assertThat(extractor.extractEffectiveRoles(jwt))
                .containsExactlyInAnyOrder("CUSTOMER", "ADMIN", "ROOT_ROLE", "ops-team");
    }

    @Test
    void extractEffectiveRoles_malformedRealmAccess_ignoredWithoutThrowing() {
        // realm_access is a String instead of the expected Map — must not throw.
        Jwt jwt = jwtWithClaims(Map.of("realm_access", "not-a-map"));

        assertThat(extractor.extractEffectiveRoles(jwt)).isEmpty();
    }

    @Test
    void extractEffectiveRoles_malformedResourceAccessEntry_ignoredWithoutThrowing() {
        // One client entry is a String instead of a Map; must be skipped, not thrown.
        Map<String, Object> resourceAccess = new HashMap<>();
        resourceAccess.put("bad-client", "not-a-map");
        resourceAccess.put("good-client", Map.of("roles", List.of("SELLER")));
        Jwt jwt = jwtWithClaims(Map.of("resource_access", resourceAccess));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactly("SELLER");
    }

    @Test
    void extractEffectiveRoles_nonStringRoleEntries_filteredOut() {
        List<Object> mixedRoles = new ArrayList<>();
        mixedRoles.add("VALID_ROLE");
        mixedRoles.add(42); // malformed / unexpected entry type
        mixedRoles.add(null);
        Map<String, Object> realmAccess = new HashMap<>();
        realmAccess.put("roles", mixedRoles);
        Jwt jwt = jwtWithClaims(Map.of("realm_access", realmAccess));

        assertThat(extractor.extractEffectiveRoles(jwt)).containsExactly("VALID_ROLE");
    }

    @Test
    void extractEffectiveRoles_customCompositeClaimRealmsPath_resolvedCorrectly() {
        // Exercises the dotted-path branch of claim resolution (e.g. a non-default
        // configuration where the realm roles claim path itself is customized).
        AppProperties customProps = new AppProperties();
        customProps.getSecurity().setClaimRealms("custom_claim.nested_roles");
        customProps.getSecurity().setClaimRoles("roles");
        JwtClaimExtractor customExtractor = new JwtClaimExtractor(customProps);

        Map<String, Object> nested = Map.of("nested_roles", List.of("CUSTOM_ROLE"));
        Jwt jwt = jwtWithClaims(Map.of("custom_claim", nested));

        assertThat(customExtractor.extractEffectiveRoles(jwt)).containsExactly("CUSTOM_ROLE");
    }
}
