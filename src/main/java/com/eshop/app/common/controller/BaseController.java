package com.eshop.app.common.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

/**
 * Base controller with common utility methods for all controllers.
 */
public abstract class BaseController {
    
    /**
     * Extract user ID from JWT token.
     */
    protected String extractUserId(Jwt jwt) {
        return jwt != null ? jwt.getSubject() : "anonymous";
    }
    
    /**
     * Extract user ID with custom default value.
     */
    protected String extractUserId(Jwt jwt, String defaultValue) {
        return jwt != null ? jwt.getSubject() : defaultValue;
    }
    
    /**
     * Extract user context with all claims.
     */
    protected UserContext extractUserContext(Jwt jwt) {
        if (jwt == null) {
            return UserContext.anonymous();
        }
        
        return UserContext.builder()
                .userId(jwt.getSubject())
                .email(jwt.getClaimAsString("email"))
                .roles(extractRoles(jwt))
                .build();
    }
    
    /**
     * Extract roles from Keycloak JWT token.
     */
    @SuppressWarnings("unchecked")
    protected Set<String> extractRoles(Jwt jwt) {
        if (jwt == null) {
            return Set.of();
        }
        
        // Handle Keycloak realm_access.roles structure
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return new HashSet<>(roles);
        }
        return Set.of();
    }
}
