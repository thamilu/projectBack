package com.eshop.app.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class for security-related operations
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<Authentication> getCurrentAuthentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    public static Optional<String> getCurrentUserId() {
        return getCurrentAuthentication().map(auth -> {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.eshop.app.security.PrincipalDetails pd) {
                return pd.getKeycloakId();
            }
            if (principal instanceof Jwt jwt) {
                return jwt.getSubject();
            }
            return null;
        }).filter(id -> id != null && !id.isBlank());
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentAuthentication().map(auth -> {
            Object principal = auth.getPrincipal();
            if (principal instanceof com.eshop.app.security.PrincipalDetails pd) {
                return pd.getUsername();
            }
            return getCurrentJwt().map(jwt -> jwt.getClaimAsString("preferred_username")).orElse(null);
        }).filter(u -> u != null && !u.isBlank());
    }

    public static boolean hasRole(String role) {
        return getCurrentAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> authorities.stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(auth -> auth.equals("ROLE_" + role) || auth.equals(role)))
                .orElse(false);
    }

    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0)
            return false;
        return getCurrentAuthentication()
                .map(Authentication::getAuthorities)
                .map(authorities -> {
                    Collection<String> userAuthorities = authorities.stream()
                            .map(GrantedAuthority::getAuthority)
                            .collect(Collectors.toSet());
                    for (String role : roles) {
                        if (userAuthorities.contains("ROLE_" + role) || userAuthorities.contains(role))
                            return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public static Long getAuthenticatedUserId() {
        return getCurrentAuthentication()
                .map(auth -> {
                    Object principal = auth.getPrincipal();
                    if (principal instanceof com.eshop.app.security.PrincipalDetails pd) {
                        return pd.getId();
                    } else if (principal instanceof Jwt jwt) {
                        // Fallback: try to parse subject as Long if no specific principal object
                        try {
                            return Long.valueOf(jwt.getSubject());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                })
                .orElseThrow(
                        () -> new org.springframework.security.access.AccessDeniedException("User not authenticated"));
    }

    public static Optional<Jwt> getCurrentJwt() {
        return getCurrentAuthentication().flatMap(auth -> {
            Object principal = auth.getPrincipal();
            if (principal instanceof Jwt jwt) {
                return Optional.of(jwt);
            }
            Object credentials = auth.getCredentials();
            if (credentials instanceof Jwt jwt) {
                return Optional.of(jwt);
            }
            if (auth instanceof org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token) {
                return Optional.ofNullable(token.getToken());
            }
            return Optional.empty();
        });
    }

}
