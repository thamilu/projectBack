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
            return switch (principal) {
                case com.eshop.app.security.PrincipalDetails pd -> pd.getKeycloakId();
                case Jwt jwt -> jwt.getSubject();
                default -> null;
            };
        }).filter(id -> id != null && !id.isBlank());
    }

    /**
     * Get the Keycloak ID (sub) of the current authenticated user.
     * Source of truth for identity sync.
     */
    public static Optional<String> getCurrentKeycloakId() {
        return getCurrentJwt().map(Jwt::getSubject);
    }

    public static Optional<String> getCurrentEmail() {
        return getCurrentAuthentication().map(auth -> {
            Object principal = auth.getPrincipal();
            return switch (principal) {
                case com.eshop.app.security.PrincipalDetails pd -> pd.getEmail();
                default -> getCurrentJwt().map(jwt -> jwt.getClaimAsString("email")).orElse(null);
            };
        }).filter(e -> e != null && !e.isBlank());
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
                    return switch (principal) {
                        case com.eshop.app.security.PrincipalDetails pd -> pd.getId();
                        case Jwt jwt -> {
                            try {
                                yield Long.valueOf(jwt.getSubject());
                            } catch (NumberFormatException e) {
                                yield null;
                            }
                        }
                        default -> null;
                    };
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
            return switch (auth) {
                case org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken token ->
                    Optional.ofNullable(token.getToken());
                default -> Optional.empty();
            };
        });
    }

}
