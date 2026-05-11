package com.eshop.app.core.permissions;

import com.eshop.app.util.SecurityUtils;
import org.springframework.stereotype.Component;

/**
 * Centralized permission policy evaluation for the E-Shop platform.
 *
 * <p>Consolidates scattered inline role checks into a single, testable,
 * auditable policy class. All complex {@code @PreAuthorize} expressions
 * should delegate to methods here rather than embedding logic in annotations.
 *
 * <p>Usage in controllers or services:
 * <pre>
 * {@code @PreAuthorize("@accessPolicy.isAdmin() or @accessPolicy.isOwner(#sellerId)") }
 * </pre>
 *
 * <p>This follows the Policy Object pattern (SOLID: Single Responsibility).
 * Adding a new permission rule means adding one method here, not hunting
 * across dozens of controllers.
 *
 * @since 2.0
 */
@Component("accessPolicy")
public class AccessPolicy {

    // ── Role Checks ──────────────────────────────────────────────────────────

    /** Returns true if the current user holds the ADMIN role. */
    public boolean isAdmin() {
        return SecurityUtils.hasRole("ADMIN");
    }

    /** Returns true if the current user holds the SELLER role. */
    public boolean isSeller() {
        return SecurityUtils.hasRole("SELLER");
    }

    /** Returns true if the current user holds the CUSTOMER role. */
    public boolean isCustomer() {
        return SecurityUtils.hasRole("CUSTOMER");
    }

    /** Returns true if the current user holds the DELIVERY role. */
    public boolean isDeliveryAgent() {
        return SecurityUtils.hasRole("DELIVERY");
    }

    /** Returns true if the current user is authenticated with any recognized role. */
    public boolean isAuthenticated() {
        return SecurityUtils.getCurrentAuthentication()
                .map(auth -> auth.isAuthenticated())
                .orElse(false);
    }

    // ── Ownership Checks ─────────────────────────────────────────────────────

    /**
     * Returns true if the current user is either an ADMIN or the owner identified
     * by {@code ownerId}. Used for "admin or self" resource access patterns.
     */
    public boolean isAdminOrOwner(Long ownerId) {
        if (isAdmin()) return true;
        return SecurityUtils.getCurrentUserId()
                .map(id -> id.equals(String.valueOf(ownerId)))
                .orElse(false);
    }

    /**
     * Returns true if the current user is either an ADMIN or a SELLER.
     * Used for endpoints accessible to both administrative and seller actors.
     */
    public boolean isAdminOrSeller() {
        return SecurityUtils.hasAnyRole("ADMIN", "SELLER");
    }
}
