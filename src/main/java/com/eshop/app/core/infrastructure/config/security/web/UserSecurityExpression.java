package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Security expression component for method-level authorization.
 *
 * <p>
 * Provides SpEL-accessible security checks for use in {@code @PreAuthorize},
 * {@code @PostAuthorize}, and {@code @PreFilter} annotations.
 * </p>
 *
 * <h3>Usage Examples:</h3>
 *
 * <pre>
 * // Check if accessing own resource
 * {@literal @}PreAuthorize("@userSecurity.isCurrentUser(#userId)")
 * public User getUser(Long userId) { ... }
 *
 * // Check ownership or admin
 * {@literal @}PreAuthorize("@userSecurity.isCurrentUserOrAdmin(#userId)")
 * public void updateUser(Long userId, UserDto dto) { ... }
 *
 * // Check store ownership (seller or admin)
 * {@literal @}PreAuthorize("@userSecurity.canManageStore(#storeId)")
 * public void updateStore(Long storeId, StoreDto dto) { ... }
 *
 * // Check order view permission (customer, store owner, or admin)
 * {@literal @}PreAuthorize("@userSecurity.canViewOrder(#orderId)")
 * public OrderDto getOrder(Long orderId) { ... }
 *
 * // Role checks
 * {@literal @}PreAuthorize("@userSecurity.isAdmin()")
 * public AdminDashboardDto getDashboard() { ... }
 *
 * {@literal @}PreAuthorize("@userSecurity.hasAnyRole('SELLER', 'ADMIN')")
 * public void manageListing() { ... }
 * </pre>
 *
 * <p>
 * This class is thread-safe and uses the ThreadLocal-based
 * SecurityContextHolder.
 * </p>
 *
 * <p><strong>Ownership checks</strong> ({@link #ownsStore}, {@link #ownsOrder},
 * {@link #isOrderStoreOwner}) use single, indexed {@code EXISTS}-style repository
 * queries rather than loading full entity graphs. This matters specifically here: these
 * methods run inside {@code @PreAuthorize} evaluation on controller methods, which are
 * typically NOT {@code @Transactional} — loading an entity and then traversing its LAZY
 * associations (e.g. {@code store.getSellerProfile().getUser()}) would risk
 * {@code LazyInitializationException} outside an active Hibernate session.</p>
 *
 * @see org.springframework.security.access.prepost.PreAuthorize
 * @see org.springframework.security.access.prepost.PostAuthorize
 */
@Slf4j
@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurityExpression {

    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final AppProperties appProperties;

    // ==================== Core User Checks ====================

    /**
     * Checks if the currently authenticated user matches the given user ID.
     *
     * @param userId the user ID to check against
     * @return true if the current user's ID matches the given userId, false
     *         otherwise
     */
    public boolean isCurrentUser(Long userId) {
        if (userId == null) {
            log.debug("isCurrentUser check failed: userId is null");
            return false;
        }

        return getCurrentUserId()
                .map(currentId -> {
                    boolean matches = currentId.equals(userId);
                    log.debug("isCurrentUser({}) for user {} = {}", userId, currentId, matches);
                    return matches;
                })
                .orElseGet(() -> {
                    log.debug("isCurrentUser({}) failed: no authenticated user", userId);
                    return false;
                });
    }

    /**
     * Checks if the currently authenticated user matches the given email.
     *
     * @param email the email to check against
     * @return true if the current user's email matches, false otherwise
     */
    public boolean isCurrentUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        return getCurrentEmail()
                .map(current -> current.equalsIgnoreCase(email))
                .orElse(false);
    }

    /**
     * Checks if the current user is the specified user OR has admin role.
     *
     * @param userId the user ID to check
     * @return true if current user matches OR is admin
     */
    public boolean isCurrentUserOrAdmin(Long userId) {
        return isAdmin() || isCurrentUser(userId);
    }

    // ==================== Role Checks ====================

    /**
     * Checks if the current user has the specified role.
     *
     * @param role the role name (without ROLE_ prefix)
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        if (role == null) {
            log.warn("hasRole called with a null role — likely a configuration error; denying.");
            return false;
        }

        String prefix = appProperties.getSecurity().getRolePrefix();
        String roleWithPrefix = role.startsWith(prefix) ? role : prefix + role;

        return getCurrentAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(roleWithPrefix));
    }

    /**
     * Checks if the current user has any of the specified roles.
     *
     * @param roles the role names to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0)
            return false;

        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if current user is an administrator.
     */
    public boolean isAdmin() {
        return hasRole(appProperties.getSecurity().getRoles().getAdmin());
    }

    /**
     * Checks if current user is a seller.
     */
    public boolean isSeller() {
        return hasRole(appProperties.getSecurity().getRoles().getSeller());
    }

    /**
     * Checks if current user is a customer.
     */
    public boolean isCustomer() {
        return hasRole(appProperties.getSecurity().getRoles().getCustomer());
    }

    /**
     * Checks if current user is a delivery agent.
     */
    public boolean isDeliveryAgent() {
        return hasRole(appProperties.getSecurity().getRoles().getDelivery());
    }

    // ==================== Resource Ownership Checks ====================

    /**
     * Checks if the current user owns the specified store.
     *
     * @param storeId the store ID to check
     * @return true if current user is the store owner
     */
    public boolean ownsStore(Long storeId) {
        if (storeId == null)
            return false;

        return getCurrentUserId()
                .map(userId -> {
                    // Single indexed EXISTS query — see class Javadoc for why this avoids
                    // loading the Store entity and traversing its LAZY sellerProfile/user
                    // associations outside a transaction.
                    boolean owns = storeRepository.existsByIdAndSellerProfile_UserId(storeId, userId);
                    log.debug("ownsStore({}) for user {} = {}", storeId, userId, owns);
                    return owns;
                })
                .orElse(false);
    }

    /**
     * Checks if the current user can manage the specified store.
     * Admins can manage all stores; sellers can manage their own.
     *
     * @param storeId the store ID
     * @return true if user can manage the store
     */
    public boolean canManageStore(Long storeId) {
        if (storeId == null)
            return false;
        return isAdmin() || ownsStore(storeId);
    }

    /**
     * Checks if the current user owns (placed) the specified order.
     *
     * @param orderId the order ID to check
     * @return true if current user placed the order
     */
    public boolean ownsOrder(Long orderId) {
        if (orderId == null)
            return false;

        return getCurrentUserId()
                .map(userId -> orderRepository.existsByIdAndCustomerId(orderId, userId))
                .orElse(false);
    }

    /**
     * Checks if the current user can view the specified order.
     * Order owners, store owners (of order items), and admins can view.
     *
     * @param orderId the order ID
     * @return true if user can view the order
     */
    public boolean canViewOrder(Long orderId) {
        if (orderId == null)
            return false;
        return isAdmin() || ownsOrder(orderId) || isOrderStoreOwner(orderId);
    }

    /**
     * Checks if the current user's store is associated with (i.e. sold a product
     * within) the specified order.
     *
     * @param orderId the order ID
     * @return true if the current user's store fulfilled any item on this order
     */
    public boolean isOrderStoreOwner(Long orderId) {
        if (orderId == null)
            return false;

        return getCurrentUserId()
                .map(userId -> orderRepository.existsOrderItemByOrderIdAndSellerUserId(orderId, userId))
                .orElse(false);
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the current authenticated user's local database ID.
     *
     * <p>Returns empty for an unresolved identity, not just an absent one:
     * {@code PrincipalDetails.id} is set to the sentinel {@code -1L} (see
     * {@link PrincipalDetails#hasResolvedId()}) when JWT-to-local-identity sync failed
     * for this request (see {@code SecurityConfig#syncUserIdentity}) — the user IS
     * authenticated, but their local ID could not be resolved. Treating {@code -1L} as a
     * real, comparable ID here would let {@link #isCurrentUser} (and every ownership
     * check built on this method) report a false-positive match between two DIFFERENT
     * users who both happen to have an unresolved identity at the same time, since
     * {@code -1L == -1L}. An unresolved identity must never be treated as "this is me."
     *
     * @return Optional containing the resolved user ID, or empty if not authenticated or
     *         the local ID could not be resolved for this request
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentPrincipal()
                .filter(PrincipalDetails::hasResolvedId)
                .map(PrincipalDetails::getId);
    }

    /**
     * Gets the current authenticated user's email.
     *
     * @return Optional containing email, or empty if not authenticated
     */
    public Optional<String> getCurrentEmail() {
        return getCurrentPrincipal()
                .map(PrincipalDetails::getEmail);
    }

    /**
     * Gets all authorities/roles of the current user.
     *
     * <p>Returns the {@link Authentication}'s own authorities collection directly
     * (Spring guarantees an immutable/defensive collection here) rather than copying
     * into a new {@link HashSet} on every call — this method is on the hot path for
     * every role check ({@link #hasRole}, {@link #isAdmin}, etc.).
     *
     * @return the current authorities, or an empty collection if not authenticated
     */
    public Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return Collections.emptySet();
        return authentication.getAuthorities();
    }

    /**
     * Retrieves the current UserDetailsImpl from the security context.
     *
     * @return Optional containing UserDetailsImpl, or empty if not available
     */
    private Optional<PrincipalDetails> getCurrentPrincipal() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null) {
                log.trace("No authentication in security context");
                return Optional.empty();
            }

            if (authentication instanceof AnonymousAuthenticationToken) {
                log.trace("Anonymous authentication detected");
                return Optional.empty();
            }

            if (!authentication.isAuthenticated()) {
                log.trace("Authentication present but not authenticated");
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();

            return switch (principal) {
                case PrincipalDetails pd -> Optional.of(pd);
                case Jwt jwt -> {
                    // Should not be reached in normal operation: SecurityConfig's
                    // jwtAuthenticationConverter always converts a Jwt into a
                    // PrincipalDetails before method security runs. Logged at ERROR
                    // (not trace) so a regression here is immediately visible rather
                    // than silently manifesting as "why can't I access my own data"
                    // support tickets — see getCurrentUserId()'s Javadoc for why a raw
                    // Jwt principal safely fails closed (denies) rather than throwing.
                    log.error("[SECURITY] Raw JWT principal in security context for subject={} — " +
                            "expected jwtAuthenticationConverter to have already resolved this to " +
                            "a PrincipalDetails. Ownership checks will deny for this request.",
                            jwt.getSubject());
                    yield extractFromJwt(jwt);
                }
                case String s -> {
                    log.trace("String principal detected: {}", s);
                    yield Optional.empty();
                }
                default -> {
                    log.error("[SECURITY] Unexpected principal type: {} — likely a security " +
                            "configuration error. Denying.", principal.getClass().getName());
                    yield Optional.empty();
                }
            };

        } catch (Exception e) {
            log.error("Error retrieving user details from security context", e);
            return Optional.empty();
        }
    }

    /**
     * Extracts user details from a JWT token.
     */
    private Optional<PrincipalDetails> extractFromJwt(Jwt jwt) {
        try {
            String subject = jwt.getSubject();

            // Note: In Keycloak, subject is a UUID string, not a numeric ID.
            // Numeric IDs are local database IDs.
            // PrincipalDetails.id should be resolved from the database using
            // findUserIdByKeycloakId.
            // If it's not available yet, we leave it null (hasResolvedId() correctly
            // treats null the same as the -1L sentinel — see getCurrentUserId()).

            return Optional.of(PrincipalDetails.builder()
                    .id(null) // ID resolution should happen in a filter or service
                    .email(jwt.getClaimAsString("email"))
                    .keycloakId(subject)
                    .build());
        } catch (Exception e) {
            log.warn("Could not extract principal from JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Checks if any user is currently authenticated (not anonymous).
     *
     * @return true if a real user is authenticated
     */
    public boolean isAuthenticated() {
        return getCurrentPrincipal().isPresent();
    }

    /**
     * Checks if the request is from an anonymous user.
     *
     * @return true if anonymous or not authenticated
     */
    public boolean isAnonymous() {
        return !isAuthenticated();
    }
}
