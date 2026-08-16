package com.eshop.app.user.shared.domain.enums;

/**
 * Defines the set of roles a user can be assigned within the e-shop platform.
 * <p>
 * This enum is a shared domain concept used across bounded contexts for
 * authorization and role-based access control decisions.
 *
 * <h2>External contract</h2>
 * <p>Constant names are part of the JWT/API/DB contract (used directly in JSON serialization,
 * {@code AppProperties.Security.Roles} defaults, and as the type of the JPA-persisted
 * {@code User.role} field, via {@code @Enumerated(EnumType.STRING)} — see {@code UserRoleTest}
 * for the persistence-safety regression guard). Do not rename an existing constant — only
 * append new ones.
 *
 * <p>This is the single, canonical role type for the platform — {@code User.role} used to be
 * backed by a separate, duplicate {@code com.eshop.app.user.domain.entity.Role} JPA enum with
 * identical constant names, requiring {@code .name()} string-matching conversion at every call
 * site with no compile-time guarantee the two stayed in sync. That duplication was removed;
 * {@code User.role} is now typed as {@code UserRole} directly.
 *
 * <h2>{@code hasRole()} vs. {@code hasAuthority()}</h2>
 * <p>These constant names carry no {@code ROLE_} prefix. Spring Security's SpEL
 * {@code hasRole(...)} expression prepends its own hardcoded {@code "ROLE_"}; this codebase
 * instead routes every authorization check through {@code SecurityExpressions}/
 * {@code UserSecurityExpression}, which build the prefix from the configurable
 * {@code AppProperties.Security#getRolePrefix()} — do not compare a raw
 * {@code GrantedAuthority} string against one of these constant names directly.
 */
public enum UserRole {

    /** Full administrative access to platform management functions. */
    ADMIN,

    /** Standard end-user role with purchasing capabilities. */
    CUSTOMER,

    /** Role for users who list and sell products on the platform. */
    SELLER,

    /** Role for users responsible for order delivery/fulfillment. */
    DELIVERY_AGENT;
}

