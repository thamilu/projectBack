package com.eshop.app.core.infrastructure.config.security.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.store.domain.repository.StoreRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Verifies the SpEL-accessible authorization checks backing every
 * {@code @PreAuthorize("@userSecurity...")} expression in the application. This class
 * had no test coverage before — it is the single most security-relevant helper in the
 * codebase (its {@code @userSecurity} bean name is referenced directly from
 * {@code @PreAuthorize} across multiple controllers).
 */
@ExtendWith(MockitoExtension.class)
class UserSecurityExpressionTest {

    @Mock private StoreRepository storeRepository;
    @Mock private OrderRepository orderRepository;

    private UserSecurityExpression security;

    @BeforeEach
    void setUp() {
        security = new UserSecurityExpression(storeRepository, orderRepository, new AppProperties());
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String... roles) {
        PrincipalDetails principal = PrincipalDetails.builder().id(userId).email("user@example.com").build();
        List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(
                java.util.Arrays.stream(roles).map(r -> "ROLE_" + r).toArray(String[]::new));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== isCurrentUser / getCurrentUserId ====================

    @Test
    void isCurrentUser_matchingId_returnsTrue() {
        authenticateAs(42L, "CUSTOMER");
        assertThat(security.isCurrentUser(42L)).isTrue();
    }

    @Test
    void isCurrentUser_differentId_returnsFalse() {
        authenticateAs(42L, "CUSTOMER");
        assertThat(security.isCurrentUser(99L)).isFalse();
    }

    @Test
    void isCurrentUser_nullTargetId_returnsFalse() {
        authenticateAs(42L, "CUSTOMER");
        assertThat(security.isCurrentUser(null)).isFalse();
    }

    @Test
    void isCurrentUser_unauthenticated_returnsFalse() {
        assertThat(security.isCurrentUser(42L)).isFalse();
    }

    @Test
    void isCurrentUser_anonymousAuthentication_returnsFalse() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymousUser",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        assertThat(security.isCurrentUser(42L)).isFalse();
    }

    @Test
    void isCurrentUser_unresolvedSentinelId_neverMatchesEvenSameSentinel() {
        // Regression guard: -1L is this codebase's established "identity sync failed"
        // sentinel (see PrincipalDetails.hasResolvedId()). Two DIFFERENT users who both
        // happen to have an unresolved identity would both carry id=-1L; comparing that
        // sentinel as if it were a real ID would make isCurrentUser(-1L) true for BOTH.
        authenticateAs(-1L, "CUSTOMER");
        assertThat(security.isCurrentUser(-1L)).isFalse();
        assertThat(security.getCurrentUserId()).isEmpty();
    }

    @Test
    void getCurrentUserId_nullPrincipalId_returnsEmpty() {
        PrincipalDetails principal = PrincipalDetails.builder().id(null).email("user@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, AuthorityUtils.createAuthorityList("ROLE_CUSTOMER")));

        assertThat(security.getCurrentUserId()).isEmpty();
    }

    @Test
    void getCurrentUserId_resolvedId_returnsIt() {
        authenticateAs(7L, "CUSTOMER");
        assertThat(security.getCurrentUserId()).contains(7L);
    }

    // ==================== isCurrentUserByEmail ====================

    @Test
    void isCurrentUserByEmail_caseInsensitiveMatch_returnsTrue() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isCurrentUserByEmail("USER@EXAMPLE.COM")).isTrue();
    }

    @Test
    void isCurrentUserByEmail_blankEmail_returnsFalse() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isCurrentUserByEmail(" ")).isFalse();
    }

    @Test
    void isCurrentUserByEmail_nullEmail_returnsFalse() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isCurrentUserByEmail(null)).isFalse();
    }

    // ==================== isCurrentUserOrAdmin ====================

    @Test
    void isCurrentUserOrAdmin_admin_returnsTrueForAnyTarget() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.isCurrentUserOrAdmin(999L)).isTrue();
    }

    @Test
    void isCurrentUserOrAdmin_selfNonAdmin_returnsTrue() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isCurrentUserOrAdmin(1L)).isTrue();
    }

    @Test
    void isCurrentUserOrAdmin_neitherSelfNorAdmin_returnsFalse() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isCurrentUserOrAdmin(2L)).isFalse();
    }

    // ==================== role checks ====================

    @Test
    void hasRole_withoutPrefix_matches() {
        authenticateAs(1L, "SELLER");
        assertThat(security.hasRole("SELLER")).isTrue();
    }

    @Test
    void hasRole_withRolePrefixAlreadyPresent_matches() {
        authenticateAs(1L, "SELLER");
        assertThat(security.hasRole("ROLE_SELLER")).isTrue();
    }

    @Test
    void hasRole_nullRole_returnsFalse() {
        authenticateAs(1L, "SELLER");
        assertThat(security.hasRole(null)).isFalse();
    }

    @Test
    void hasAnyRole_oneOfManyMatches_returnsTrue() {
        authenticateAs(1L, "SELLER");
        assertThat(security.hasAnyRole("ADMIN", "SELLER")).isTrue();
    }

    @Test
    void hasAnyRole_noMatches_returnsFalse() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.hasAnyRole("ADMIN", "SELLER")).isFalse();
    }

    @Test
    void hasAnyRole_emptyArray_returnsFalse() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.hasAnyRole()).isFalse();
    }

    @Test
    void isAdmin_isSeller_isCustomer_isDeliveryAgent_useConfiguredRoleNames() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.isAdmin()).isTrue();
        assertThat(security.isSeller()).isFalse();
        assertThat(security.isCustomer()).isFalse();
        assertThat(security.isDeliveryAgent()).isFalse();
    }

    @Test
    void isDeliveryAgent_matchesConfiguredDeliveryRole() {
        authenticateAs(1L, "DELIVERY_AGENT");
        assertThat(security.isDeliveryAgent()).isTrue();
    }

    @Test
    void hasRole_respectsCustomConfiguredPrefix() {
        AppProperties customProps = new AppProperties();
        customProps.getSecurity().setRolePrefix("AUTH_");
        UserSecurityExpression customSecurity =
                new UserSecurityExpression(storeRepository, orderRepository, customProps);

        PrincipalDetails principal = PrincipalDetails.builder().id(1L).email("user@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null,
                        AuthorityUtils.createAuthorityList("AUTH_ADMIN")));

        assertThat(customSecurity.hasRole("ADMIN")).isTrue();
    }

    // ==================== ownsStore / canManageStore ====================
    // NOTE: ownership checks now delegate to single indexed EXISTS-style repository
    // methods (existsByIdAndSellerProfile_UserId, existsByIdAndCustomerId,
    // existsOrderItemByOrderIdAndSellerUserId) instead of loading the full entity graph
    // and traversing LAZY associations — see the class Javadoc for why. Tests mock the
    // repository boolean directly rather than building deep entity graphs.

    @Test
    void ownsStore_matchingOwner_returnsTrue() {
        authenticateAs(5L, "SELLER");
        when(storeRepository.existsByIdAndSellerProfile_UserId(10L, 5L)).thenReturn(true);

        assertThat(security.ownsStore(10L)).isTrue();
    }

    @Test
    void ownsStore_differentOwner_returnsFalse() {
        authenticateAs(5L, "SELLER");
        when(storeRepository.existsByIdAndSellerProfile_UserId(10L, 5L)).thenReturn(false);

        assertThat(security.ownsStore(10L)).isFalse();
    }

    @Test
    void ownsStore_nullStoreId_returnsFalse() {
        authenticateAs(5L, "SELLER");
        assertThat(security.ownsStore(null)).isFalse();
    }

    @Test
    void canManageStore_admin_bypassesOwnershipCheck() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.canManageStore(999L)).isTrue();
    }

    @Test
    void canManageStore_nullStoreId_returnsFalse() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.canManageStore(null)).isFalse();
    }

    // ==================== ownsOrder / isOrderStoreOwner / canViewOrder ====================

    @Test
    void ownsOrder_matchingCustomer_returnsTrue() {
        authenticateAs(3L, "CUSTOMER");
        when(orderRepository.existsByIdAndCustomerId(20L, 3L)).thenReturn(true);

        assertThat(security.ownsOrder(20L)).isTrue();
    }

    @Test
    void ownsOrder_differentCustomer_returnsFalse() {
        authenticateAs(3L, "CUSTOMER");
        when(orderRepository.existsByIdAndCustomerId(20L, 3L)).thenReturn(false);

        assertThat(security.ownsOrder(20L)).isFalse();
    }

    @Test
    void isOrderStoreOwner_sellerOwnsProductStoreInOrder_returnsTrue() {
        authenticateAs(8L, "SELLER");
        when(orderRepository.existsOrderItemByOrderIdAndSellerUserId(30L, 8L)).thenReturn(true);

        assertThat(security.isOrderStoreOwner(30L)).isTrue();
    }

    @Test
    void isOrderStoreOwner_unrelatedSeller_returnsFalse() {
        authenticateAs(8L, "SELLER");
        when(orderRepository.existsOrderItemByOrderIdAndSellerUserId(30L, 8L)).thenReturn(false);

        assertThat(security.isOrderStoreOwner(30L)).isFalse();
    }

    @Test
    void canViewOrder_admin_bypassesOwnershipChecks() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.canViewOrder(999L)).isTrue();
    }

    @Test
    void canViewOrder_nullOrderId_returnsFalse() {
        authenticateAs(1L, "ADMIN");
        assertThat(security.canViewOrder(null)).isFalse();
    }

    // ==================== isAuthenticated / isAnonymous ====================

    @Test
    void isAuthenticated_withValidPrincipal_returnsTrue() {
        authenticateAs(1L, "CUSTOMER");
        assertThat(security.isAuthenticated()).isTrue();
        assertThat(security.isAnonymous()).isFalse();
    }

    @Test
    void isAuthenticated_noAuthentication_returnsFalse() {
        assertThat(security.isAuthenticated()).isFalse();
        assertThat(security.isAnonymous()).isTrue();
    }
}
