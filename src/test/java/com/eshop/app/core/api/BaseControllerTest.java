package com.eshop.app.core.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.eshop.app.core.security.UserContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BaseControllerTest {

    private final BaseController controller = new BaseController() {};

    @Mock Jwt jwt;

    @Test
    void extractUserId_returnsAnonymousWhenJwtNull() {
        assertEquals(UserContext.ANONYMOUS_USER_ID, controller.extractUserId(null));
    }

    @Test
    void extractUserId_withCustomDefault_returnsDefaultWhenJwtNull() {
        assertEquals("guest", controller.extractUserId(null, "guest"));
    }

    @Test
    void extractUserId_returnsSubjectWhenJwtPresent() {
        when(jwt.getSubject()).thenReturn("user-123");
        assertEquals("user-123", controller.extractUserId(jwt));
    }

    @Test
    void extractUserContext_returnsAnonymousWhenJwtNull() {
        UserContext context = controller.extractUserContext(null);
        assertEquals(UserContext.ANONYMOUS_USER_ID, context.userId());
        assertTrue(context.roles().isEmpty());
    }

    @Test
    void extractUserContext_populatesFromJwtClaims() {
        when(jwt.getSubject()).thenReturn("user-123");
        when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
        when(jwt.getClaim("realm_access"))
                .thenReturn(Map.of("roles", List.of("ADMIN", "SELLER")));

        UserContext context = controller.extractUserContext(jwt);

        assertEquals("user-123", context.userId());
        assertEquals("user@example.com", context.email());
        assertEquals(Set.of("ADMIN", "SELLER"), context.roles());
        assertTrue(context.isAdmin());
    }

    @Test
    void extractRoles_returnsEmptyWhenJwtNull() {
        assertTrue(controller.extractRoles(null).isEmpty());
    }

    @Test
    void extractRoles_returnsEmptyWhenRealmAccessClaimAbsent() {
        when(jwt.getClaim("realm_access")).thenReturn(null);
        assertTrue(controller.extractRoles(jwt).isEmpty());
    }

    @Test
    void extractRoles_returnsEmptyWhenRealmAccessIsNotAMap() {
        when(jwt.getClaim("realm_access")).thenReturn("not-a-map");
        assertTrue(controller.extractRoles(jwt).isEmpty());
    }

    @Test
    void extractRoles_returnsEmptyWhenRolesClaimIsNotAList() {
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", "ADMIN"));
        assertTrue(controller.extractRoles(jwt).isEmpty());
    }

    @Test
    void extractRoles_ignoresNonStringEntriesRatherThanThrowing() {
        when(jwt.getClaim("realm_access"))
                .thenReturn(Map.of("roles", List.of("ADMIN", 42, "SELLER")));

        Set<String> roles = controller.extractRoles(jwt);

        assertEquals(Set.of("ADMIN", "SELLER"), roles);
    }

    @Test
    void extractRoles_returnedSetIsUnmodifiable() {
        when(jwt.getClaim("realm_access")).thenReturn(Map.of("roles", List.of("ADMIN")));

        Set<String> roles = controller.extractRoles(jwt);

        assertThrows(UnsupportedOperationException.class, () -> roles.add("HACKED"));
    }
}
