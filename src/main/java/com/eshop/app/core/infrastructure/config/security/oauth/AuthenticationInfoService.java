package com.eshop.app.core.infrastructure.config.security.oauth;

import com.eshop.app.core.infrastructure.config.response.ConfigResponse;
import com.eshop.app.core.exception.security.UnauthorizedException;

import com.eshop.app.core.infrastructure.config.properties.KeycloakConfigProperties;
import com.eshop.app.user.api.response.UserInfoResponse;
import com.eshop.app.user.api.response.TokenInfoResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * Service for extracting and building authentication information from JWT
 * tokens.
 * Centralizes authority extraction and user info building logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationInfoService {

    private final KeycloakConfigProperties keycloakConfig;

    private volatile ConfigResponse cachedPublicConfig;

    @PostConstruct
    void init() {
        log.info("Initializing AuthenticationInfoService");
        buildPublicConfig();
    }

    /**
     * Builds and caches public Keycloak configuration.
     */
    private void buildPublicConfig() {
        Map<String, Object> cfg = new HashMap<>();

        if (keycloakConfig != null) {
            putIfNotNull(cfg, "realm", keycloakConfig.getRealm());
            putIfNotNull(cfg, "authUrl", keycloakConfig.getAuthUrl());
            putIfNotNull(cfg, "clientId", keycloakConfig.getResource());
        }

        this.cachedPublicConfig = new ConfigResponse(
                (String) cfg.get("realm"),
                (String) cfg.get("authUrl"),
                (String) cfg.get("clientId"));

        log.debug("Public config cached: realm={}, authUrl={}",
                cachedPublicConfig.realm(), cachedPublicConfig.authUrl());
    }

    private void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Gets cached public configuration.
     */
    public ConfigResponse getPublicConfig() {
        return cachedPublicConfig;
    }

    /**
     * Builds user information from JWT token.
     * 
     * @param jwt            The JWT token
     * @param authentication The authentication object
     * @return User information response
     * @throws UnauthorizedException if JWT is null
     */
    public UserInfoResponse buildUserInfo(Jwt jwt, Authentication authentication) {
        if (jwt == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        List<String> roles = extractAuthorities(authentication);

        return UserInfoResponse.builder()
                .username(getClaimOrDefault(jwt, "preferred_username", "unknown"))
                .email(getClaimOrDefault(jwt, "email", null))
                .firstName(getClaimOrDefault(jwt, "given_name", null))
                .lastName(getClaimOrDefault(jwt, "family_name", null))
                .fullName(getClaimOrDefault(jwt, "name", null))
                .roles(roles)
                .emailVerified(Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")))
                .sub(jwt.getSubject())
                .build();
    }

    /**
     * Builds token validation information.
     * 
     * @param jwt            The JWT token
     * @param authentication The authentication object
     * @return Token information response
     * @throws UnauthorizedException if JWT is null
     */
    public TokenInfoResponse buildTokenInfo(Jwt jwt, Authentication authentication) {
        if (jwt == null) {
            throw new UnauthorizedException("Invalid or expired token");
        }

        List<String> authorities = extractAuthorities(authentication);

        return new TokenInfoResponse(
                true,
                getClaimOrDefault(jwt, "preferred_username", "unknown"),
                jwt.getExpiresAt(),
                jwt.getIssuedAt(),
                authorities);
    }

    /**
     * Extracts granted authorities from authentication object.
     * 
     * @param authentication The authentication object (can be null)
     * @return List of authority strings
     */
    public List<String> extractAuthorities(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList(); // Java 21: immutable list
    }

    /**
     * Safely extracts JWT claim with default value.
     * 
     * @param jwt          The JWT token
     * @param claimName    The claim name
     * @param defaultValue Default value if claim is null
     * @return Claim value or default
     */
    private String getClaimOrDefault(Jwt jwt, String claimName, String defaultValue) {
        String value = jwt.getClaimAsString(claimName);
        return value != null ? value : defaultValue;
    }
}


