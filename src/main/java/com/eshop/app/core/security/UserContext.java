package com.eshop.app.core.security;

import lombok.Builder;

import java.util.Set;

/**
 * User context extracted from JWT token.
 */
@Builder
public record UserContext(
        String userId,
        String email,
        Set<String> roles) {

    public static final String ANONYMOUS_USER_ID = "anonymous";

    public static UserContext anonymous() {
        return new UserContext(ANONYMOUS_USER_ID, null, Set.of());
    }

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isSeller() {
        return hasRole("SELLER");
    }
}
