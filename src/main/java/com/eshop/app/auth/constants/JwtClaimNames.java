package com.eshop.app.auth.constants;

/**
 * JWT claim names for Keycloak tokens.
 * Prevents magic strings throughout the codebase.
 */
public final class JwtClaimNames {
    
    public static final String PREFERRED_USERNAME = "preferred_username";
    public static final String EMAIL = "email";
    public static final String GIVEN_NAME = "given_name";
    public static final String FAMILY_NAME = "family_name";
    public static final String NAME = "name";
    public static final String EMAIL_VERIFIED = "email_verified";
    public static final String REALM_ACCESS = "realm_access";
    public static final String RESOURCE_ACCESS = "resource_access";
    public static final String ROLES = "roles";
    public static final String SUB = "sub";
    
    private JwtClaimNames() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
