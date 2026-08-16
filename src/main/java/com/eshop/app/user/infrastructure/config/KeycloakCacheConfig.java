package com.eshop.app.user.infrastructure.config;

import com.eshop.app.dto.security.UserPrincipalCache;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.keycloak.representations.idm.RoleRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Caffeine in-memory {@link Cache} beans used to reduce redundant
 * calls to the Keycloak Admin REST API.
 *
 * <p>Two independent caches are configured:
 * <ul>
 *   <li>{@code keycloakRoleCache} - caches {@link RoleRepresentation} lookups,
 *       keyed by the role identifier used by the calling repository/service.</li>
 *   <li>{@code userPrincipalCache} - caches resolved {@link UserPrincipalCache}
 *       security context objects, keyed by the authenticated user identifier
 *       used by the calling service.</li>
 * </ul>
 *
 * <p><b>Security note:</b> both caches expire strictly on a write-based TTL.
 * If a role or user entitlement is modified/revoked directly in Keycloak,
 * the change will NOT be reflected in this application until the cached
 * entry naturally expires (up to {@code role-ttl-minutes} /
 * {@code user-principal-ttl-minutes}). Any service that mutates roles or
 * user entitlements via the Keycloak Admin API is responsible for calling
 * {@code cache.invalidate(key)} (or {@code invalidateAll()}) on the
 * relevant cache to avoid serving stale authorization data.
 */
@Configuration
public class KeycloakCacheConfig {

    private static final Logger log = LoggerFactory.getLogger(KeycloakCacheConfig.class);

    /**
     * Bounded, time-based cache for Keycloak {@link RoleRepresentation} lookups.
     *
     * @param ttlMinutes time-to-live, in minutes, measured from last write
     * @param maxSize    maximum number of entries retained before size-based eviction
     * @return a Caffeine cache with statistics recording enabled
     */
    @Bean
    public Cache<String, RoleRepresentation> keycloakRoleCache(
            @Value("${keycloak.cache.role-ttl-minutes:30}") int ttlMinutes,
            @Value("${keycloak.cache.role-max-size:1000}") int maxSize) {
        log.info("Initializing Keycloak role cache: ttlMinutes={}, maxSize={}", ttlMinutes, maxSize);
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }

    /**
     * Bounded, time-based cache for resolved {@link UserPrincipalCache}
     * security context objects.
     *
     * @param ttlMinutes time-to-live, in minutes, measured from last write
     * @param maxSize    maximum number of entries retained before size-based eviction
     * @return a Caffeine cache with statistics recording enabled
     */
    @Bean
    public Cache<String, UserPrincipalCache> userPrincipalCache(
            @Value("${keycloak.cache.user-principal-ttl-minutes:5}") int ttlMinutes,
            @Value("${keycloak.cache.user-principal-max-size:10000}") int maxSize) {
        log.info("Initializing Keycloak user principal cache: ttlMinutes={}, maxSize={}", ttlMinutes, maxSize);
        return Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(ttlMinutes, TimeUnit.MINUTES)
                .recordStats()
                .build();
    }
}

