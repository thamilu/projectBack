package com.eshop.app.config;

import com.eshop.app.repository.OrderRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.security.PrincipalDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Security expression component for method-level authorization.
 * 
 * <p>Provides SpEL-accessible security checks for use in {@code @PreAuthorize},
 * {@code @PostAuthorize}, and {@code @PreFilter} annotations.</p>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>
 * // Check if accessing own resource
 * {@literal @}PreAuthorize("@userSecurity.isCurrentUser(#userId)")
 * public User getUser(Long userId) { ... }
 * 
 * // Check ownership or admin
 * {@literal @}PreAuthorize("@userSecurity.isCurrentUserOrAdmin(#userId)")
 * public void updateUser(Long userId, UserDto dto) { ... }
 * 
 * // Check shop ownership
 * {@literal @}PreAuthorize("@userSecurity.ownsShop(#shopId)")
 * public void updateShop(Long shopId, ShopDto dto) { ... }
 * </pre>
 * 
 * <p>This class is thread-safe and uses the ThreadLocal-based SecurityContextHolder.</p>
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

    // ==================== Core User Checks ====================

    /**
     * Checks if the currently authenticated user matches the given user ID.
     *
     * @param userId the user ID to check against
     * @return true if the current user's ID matches the given userId, false otherwise
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
     * Checks if the currently authenticated user matches the given username.
     *
     * @param username the username to check against
     * @return true if the current user's username matches, false otherwise
     */
    public boolean isCurrentUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        return getCurrentUsername()
                .map(current -> current.equalsIgnoreCase(username))
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
        if (role == null) return false;

        String roleWithPrefix = role.startsWith("ROLE_") ? role : "ROLE_" + role;

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
        if (roles == null || roles.length == 0) return false;

        Set<String> roleSet = Arrays.stream(roles)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .collect(Collectors.toSet());

        return getCurrentAuthorities()
                .stream()
                .anyMatch(a -> roleSet.contains(a.getAuthority()));
    }

    /**
     * Checks if current user is an administrator.
     */
    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * Checks if current user is a seller.
     */
    public boolean isSeller() {
        return hasRole("SELLER");
    }

    /**
     * Checks if current user is a customer.
     */
    public boolean isCustomer() {
        return hasRole("CUSTOMER");
    }

    // ==================== Resource Ownership Checks ====================

    /**
     * Checks if the current user owns the specified shop.
     *
     * @param shopId the shop ID to check
     * @return true if current user is the shop owner
     */
    public boolean ownsStore(Long storeId) {
        if (storeId == null) return false;

        return getCurrentUserId()
                .map(userId -> {
                    boolean owns = storeRepository.findById(storeId)
                            .map(s -> s.getSellerProfile() != null && s.getSellerProfile().getUser() != null
                                    && Objects.equals(s.getSellerProfile().getUser().getId(), userId))
                            .orElse(false);
                    log.debug("ownsStore({}) for user {} = {}", storeId, userId, owns);
                    return owns;
                })
                .orElse(false);
    }

    /**
     * Checks if the current user can manage the specified shop.
     * Admins can manage all shops; sellers can manage their own.
     *
     * @param shopId the shop ID
     * @return true if user can manage the shop
     */
    public boolean canManageStore(Long storeId) {
        return isAdmin() || ownsStore(storeId);
    }

    /**
     * Checks if the current user owns the specified order.
     *
     * @param orderId the order ID to check
     * @return true if current user placed the order
     */
    public boolean ownsOrder(Long orderId) {
        if (orderId == null) return false;

        return getCurrentUserId()
            .map(userId -> orderRepository.findById(orderId)
                .map(o -> o.getCustomer() != null && Objects.equals(o.getCustomer().getId(), userId))
                .orElse(false))
            .orElse(false);
    }

    /**
     * Checks if the current user can view the specified order.
     * Order owners, shop owners (of order items), and admins can view.
     *
     * @param orderId the order ID
     * @return true if user can view the order
     */
    public boolean canViewOrder(Long orderId) {
        return isAdmin() || ownsOrder(orderId) || isOrderStoreOwner(orderId);
    }

    /**
     * Checks if the current user's shop is associated with the order.
     */
    public boolean isOrderStoreOwner(Long orderId) {
        if (orderId == null) return false;

        return getCurrentUserId()
            .map(userId -> orderRepository.findById(orderId)
                .map(o -> o.getItems().stream()
                    .anyMatch(i -> i.getProduct() != null
                        && i.getProduct().getStore() != null
                                        && i.getProduct().getStore().getSellerProfile() != null
                                        && i.getProduct().getStore().getSellerProfile().getUser() != null
                                        && Objects.equals(
                                                i.getProduct().getStore().getSellerProfile().getUser().getId(),
                                                userId)))
                .orElse(false))
            .orElse(false);
    }

    // ==================== Helper Methods ====================

    /**
     * Gets the current authenticated user's ID.
     *
     * @return Optional containing user ID, or empty if not authenticated
     */
    public Optional<Long> getCurrentUserId() {
        return getCurrentPrincipal()
                .map(PrincipalDetails::getId);
    }

    /**
     * Gets the current authenticated user's username.
     *
     * @return Optional containing username, or empty if not authenticated
     */
    public Optional<String> getCurrentUsername() {
        return getCurrentPrincipal()
                .map(PrincipalDetails::getUsername);
    }

    /**
     * Gets all authorities/roles of the current user.
     *
     * @return Set of authorities, empty set if not authenticated
     */
    public Set<GrantedAuthority> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
            return Collections.emptySet();
        return new HashSet<>(authentication.getAuthorities());
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

            if (principal instanceof PrincipalDetails pd) {
                return Optional.of(pd);
            }

            if (principal instanceof Jwt jwt) {
                log.trace("JWT principal detected, extracting user details");
                return extractFromJwt(jwt);
            }

            if (principal instanceof String) {
                log.trace("String principal detected: {}", principal);
                return Optional.empty();
            }

            log.warn("Unknown principal type: {}", principal.getClass().getName());
            return Optional.empty();

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
            // PrincipalDetails.id should be resolved from the database using findUserIdByKeycloakId.
            // If it's not available yet, we leave it null or -1L.
            
            return Optional.of(PrincipalDetails.builder()
                    .id(null) // ID resolution should happen in a filter or service
                    .username(jwt.getClaimAsString("preferred_username"))
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