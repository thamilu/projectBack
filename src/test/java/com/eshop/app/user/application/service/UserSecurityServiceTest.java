package com.eshop.app.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Verifies the service-layer authorization guards used by {@code UserCommandService}.
 * These are a defense-in-depth layer independent of controller-level
 * {@code @PreAuthorize}/{@code UserSecurityExpression} checks.
 */
class UserSecurityServiceTest {

    private UserSecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new UserSecurityService(new AppProperties());
    }

    private Authentication authAs(Long userId, String... roles) {
        Authentication auth = mock(Authentication.class);
        PrincipalDetails principal = PrincipalDetails.builder().id(userId).email("user@example.com").build();
        when(auth.getPrincipal()).thenReturn(principal);
        List<SimpleGrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority(r.startsWith("ROLE_") ? r : "ROLE_" + r))
                .toList();
        org.mockito.Mockito.doReturn(authorities).when(auth).getAuthorities();
        return auth;
    }

    // ==================== checkCanModifyUser ====================

    @Test
    void checkCanModifyUser_admin_canModifyAnyUser() {
        Authentication admin = authAs(1L, "ADMIN");

        assertThatCode(() -> securityService.checkCanModifyUser(999L, admin)).doesNotThrowAnyException();
    }

    @Test
    void checkCanModifyUser_selfTarget_allowed() {
        Authentication user = authAs(42L, "CUSTOMER");

        assertThatCode(() -> securityService.checkCanModifyUser(42L, user)).doesNotThrowAnyException();
    }

    @Test
    void checkCanModifyUser_differentTarget_denied() {
        Authentication user = authAs(42L, "CUSTOMER");

        assertThatThrownBy(() -> securityService.checkCanModifyUser(99L, user))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkCanModifyUser_unresolvedIdentity_deniesInsteadOfThrowingNpe() {
        // PrincipalDetails.getId() can legitimately be null for an unresolved local
        // identity (see UserController's JIT-sync fallback). Previously this threw an
        // unhandled NullPointerException from currentUserId.equals(targetUserId);
        // it must now cleanly deny instead.
        Authentication user = authAs(null, "CUSTOMER");

        assertThatThrownBy(() -> securityService.checkCanModifyUser(42L, user))
                .isInstanceOf(AccessDeniedException.class)
                .isNotInstanceOf(NullPointerException.class);
    }

    @Test
    void checkCanModifyUser_unresolvedIdentityAndNullTarget_doesNotThrowNpe() {
        // Objects.equals(null, null) is true - both sides unresolved is treated as a
        // match rather than crashing; still exercised here as a defensive regression guard.
        Authentication user = authAs(null, "CUSTOMER");

        assertThatCode(() -> securityService.checkCanModifyUser(null, user)).doesNotThrowAnyException();
    }

    // ==================== checkCanChangeRole ====================

    @Test
    void checkCanChangeRole_admin_allowed() {
        assertThatCode(() -> securityService.checkCanChangeRole(authAs(1L, "ADMIN"))).doesNotThrowAnyException();
    }

    @Test
    void checkCanChangeRole_nonAdmin_denied() {
        assertThatThrownBy(() -> securityService.checkCanChangeRole(authAs(1L, "CUSTOMER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ==================== checkCanPerformBulkOperation ====================

    @Test
    void checkCanPerformBulkOperation_nonAdmin_denied() {
        assertThatThrownBy(() -> securityService.checkCanPerformBulkOperation(5, authAs(1L, "CUSTOMER")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void checkCanPerformBulkOperation_adminWithinLimit_allowed() {
        assertThatCode(() -> securityService.checkCanPerformBulkOperation(50, authAs(1L, "ADMIN")))
                .doesNotThrowAnyException();
    }

    @Test
    void checkCanPerformBulkOperation_adminExceedsConfiguredLimit_throwsBusinessException() {
        // Default configured limit is 1000 (AppProperties.Security#bulkOperationMaxSize).
        assertThatThrownBy(() -> securityService.checkCanPerformBulkOperation(1001, authAs(1L, "ADMIN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void checkCanPerformBulkOperation_respectsConfiguredLimitOverride() {
        AppProperties props = new AppProperties();
        props.getSecurity().setBulkOperationMaxSize(10);
        UserSecurityService customLimitService = new UserSecurityService(props);

        assertThatThrownBy(() -> customLimitService.checkCanPerformBulkOperation(11, authAs(1L, "ADMIN")))
                .isInstanceOf(BusinessException.class);
        assertThatCode(() -> customLimitService.checkCanPerformBulkOperation(10, authAs(1L, "ADMIN")))
                .doesNotThrowAnyException();
    }

    // ==================== extractUserId ====================

    @Test
    void extractUserId_validPrincipal_returnsId() {
        assertThat(securityService.extractUserId(authAs(7L, "CUSTOMER"))).isEqualTo(7L);
    }

    @Test
    void extractUserId_nullAuthentication_throwsAccessDeniedNotIllegalState() {
        // Must map to a clean 403 (AccessDeniedException, handled by GlobalExceptionHandler)
        // rather than an unmapped IllegalStateException (falls through to a raw 500).
        assertThatThrownBy(() -> securityService.extractUserId(null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void extractUserId_nonPrincipalDetailsPrincipal_throwsAccessDenied() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("anonymousUser");

        assertThatThrownBy(() -> securityService.extractUserId(auth))
                .isInstanceOf(AccessDeniedException.class);
    }
}
