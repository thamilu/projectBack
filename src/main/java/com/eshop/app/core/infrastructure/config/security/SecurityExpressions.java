package com.eshop.app.core.infrastructure.config.security;

/**
 * Centralized Spring Security {@code @PreAuthorize} SpEL expressions, kept in one place so
 * role-name and prefix configuration cannot drift between call sites (see
 * {@code AppProperties.Security} for the configurable role names/prefix, and
 * {@code UserSecurityExpression} — bean name {@code userSecurity} — for the checks these
 * expressions delegate to).
 *
 * <p>Role checks route through {@code @userSecurity.isXxx()} rather than the built-in
 * {@code hasRole(...)}/{@code hasAuthority(...)} SpEL functions. Spring Security's {@code hasRole()}
 * always applies its own hardcoded {@code "ROLE_"} prefix, independent of
 * {@code AppProperties.Security#getRolePrefix()}; if that prefix is ever changed, {@code hasRole(...)}
 * and {@code userSecurity}'s configurable-prefix checks would silently diverge. {@code userSecurity}
 * always uses the configured prefix, so routing every expression through it keeps a single source
 * of truth.
 */
public final class SecurityExpressions {

    /** True if the caller holds the configured admin role. */
    public static final String IS_ADMIN = "@userSecurity.isAdmin()";

    /** True if the caller holds the configured seller role. */
    public static final String IS_SELLER = "@userSecurity.isSeller()";

    /** True if the caller holds the configured customer role. */
    public static final String IS_CUSTOMER = "@userSecurity.isCustomer()";

    /** True if the caller holds the configured delivery-agent role. */
    public static final String IS_DELIVERY_AGENT = "@userSecurity.isDeliveryAgent()";

    /**
     * True for an admin, or for the user themselves. Requires the handler method to bind the
     * target user id to a parameter named {@code id} (e.g. {@code @PathVariable Long id}).
     */
    public static final String IS_ADMIN_OR_SELF = IS_ADMIN + " or @userSecurity.isCurrentUser(#id)";

    /**
     * Same as {@link #IS_ADMIN_OR_SELF}, for handler methods whose target-user parameter is
     * named {@code userId} rather than {@code id} — both naming conventions are in active use
     * across this codebase (e.g. {@code ProductReviewController}, {@code ShippingController}
     * use {@code #userId}; {@code UserController}, {@code ShoppingCartController} use {@code #id}).
     */
    public static final String IS_ADMIN_OR_SELF_BY_USER_ID =
            IS_ADMIN + " or @userSecurity.isCurrentUser(#userId)";

    /**
     * Same as {@link #IS_ADMIN_OR_SELF}, keyed by an email parameter named {@code email} instead
     * of a user id.
     */
    public static final String IS_ADMIN_OR_SELF_BY_EMAIL =
            IS_ADMIN + " or @userSecurity.isCurrentUserByEmail(#email)";

    /**
     * True for an admin, or the seller who owns the store identified by a {@code storeId}
     * parameter.
     */
    public static final String CAN_MANAGE_STORE = "@userSecurity.canManageStore(#storeId)";

    /**
     * True for an admin, the customer who placed the order, or the seller of a product in the
     * order — all identified by an {@code orderId} parameter.
     */
    public static final String CAN_VIEW_ORDER = "@userSecurity.canViewOrder(#orderId)";

    /** True for an admin or a seller. */
    public static final String IS_ADMIN_OR_SELLER = IS_ADMIN + " or " + IS_SELLER;

    /** True for an admin or a customer. */
    public static final String IS_ADMIN_OR_CUSTOMER = IS_ADMIN + " or " + IS_CUSTOMER;

    /** True for an admin or a delivery agent. */
    public static final String IS_ADMIN_OR_DELIVERY_AGENT = IS_ADMIN + " or " + IS_DELIVERY_AGENT;

    /** True for an admin, seller, or customer. */
    public static final String IS_ADMIN_OR_SELLER_OR_CUSTOMER =
            IS_ADMIN + " or " + IS_SELLER + " or " + IS_CUSTOMER;

    /** True for an admin, seller, or delivery agent. */
    public static final String IS_ADMIN_OR_SELLER_OR_DELIVERY_AGENT =
            IS_ADMIN + " or " + IS_SELLER + " or " + IS_DELIVERY_AGENT;

    /** True for an admin, customer, or delivery agent. */
    public static final String IS_ADMIN_OR_CUSTOMER_OR_DELIVERY_AGENT =
            IS_ADMIN + " or " + IS_CUSTOMER + " or " + IS_DELIVERY_AGENT;

    /** True for any authenticated holder of one of the four platform roles. */
    public static final String IS_ANY_ROLE =
            IS_ADMIN + " or " + IS_SELLER + " or " + IS_CUSTOMER + " or " + IS_DELIVERY_AGENT;

    /**
     * True for an admin, or the seller themselves when {@code #sellerId} matches the caller's id
     * (compared directly against {@code principal.id}, matching this expression's pre-existing
     * call sites — {@code ProductAnalyticsService}/{@code DefaultProductService}'s seller
     * analytics endpoints).
     */
    public static final String IS_ADMIN_OR_SELLER_SELF_BY_SELLER_ID =
            IS_ADMIN + " or (" + IS_SELLER + " and #sellerId == principal.id)";

    /**
     * True for an admin, or a seller who owns the product identified by an {@code id} parameter
     * (ownership resolved via {@code @productSecurityService.isOwner}).
     */
    public static final String CAN_MANAGE_PRODUCT =
            IS_ADMIN + " or (" + IS_SELLER + " and @productSecurityService.isOwner(#id, principal))";

    /**
     * True for an admin, or a customer acting on their own resource (identified by an {@code id}
     * parameter via {@code @userSecurity.isCurrentUser}). Equivalent by Boolean distribution to
     * {@code (isCustomer() or isAdmin()) and (isCurrentUser(#id) or isAdmin())}, the shape this
     * replaces in {@code ShoppingCartController}.
     */
    public static final String IS_ADMIN_OR_SELF_CUSTOMER =
            IS_ADMIN + " or (" + IS_CUSTOMER + " and @userSecurity.isCurrentUser(#id))";

    public static final String IS_AUTHENTICATED = "isAuthenticated()";

    private SecurityExpressions() {}
}
