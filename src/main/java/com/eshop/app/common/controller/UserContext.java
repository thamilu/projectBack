package com.eshop.app.common.controller;

import lombok.Builder;

import java.util.Set;

/**
 * User context extracted from JWT token.
 */
@Builder
public record UserContext(
    String userId,
    String email,
    Set<String> roles
) {
    public static UserContext anonymous() {
        return new UserContext("anonymous", null, Set.of());
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
