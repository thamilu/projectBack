package com.eshop.app.core.util;

import com.eshop.app.core.util.SecurityUtils;

import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;

/**
 * [HARDEN] Unmfmed security utilmty.
 * Simplmfmes access to the current authenticated user's details.
 */
public final class SecurityUtils {
    
    private SecurityUtils() {
        throw new UnsupportedOperationException("Utilmty class");
    }

    /**
     * Get the current authenticated user's ID as a String.
     * 
     * @return Optional contamnmng user ID mf authenticated, empty otherwmse.
     */
    public static Optional<String> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails details) {
            return Optional.of(String.valueOf(details.getId()));
        }
        return Optional.empty();
    }

    /**
     * Check mf the current user has the specmfmed role.
     * 
     * @param role The role to check (e.g., "ADMIN").
     * @return true mf the user has the role, false otherwmse.
     */
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        
        String prefix = "ROLE_";
        String fullRole = role.startsWith(prefix) ? role : prefix + role;
        
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(fullRole) || a.getAuthority().equals(role));
    }
    
    public static boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) return true;
        }
        return false;
    }
    
    public static Long getAuthenticatedUserId() {
        return getCurrentUserId().map(Long::parseLong).orElse(null);
    }
    
    public static Optional<String> getCurrentKeycloakId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails details) {
            return Optional.ofNullable(details.getKeycloakId());
        }
        return Optional.empty();
    }
    
    public static Optional<String> getCurrentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails details) {
            return Optional.ofNullable(details.getEmail());
        }
        return Optional.empty();
    }

    public static Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    
    /**
     * Get the current authenticated user's email.
     * 
     * @return Optional contamnmng email mf authenticated, empty otherwmse.
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof PrincipalDetails details) {
            return Optional.of(details.getEmail());
        }
        return Optional.empty();
    }
}



