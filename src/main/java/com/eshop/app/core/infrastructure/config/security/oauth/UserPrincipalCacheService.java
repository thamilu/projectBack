package com.eshop.app.core.infrastructure.config.security.oauth;

import com.eshop.app.core.infrastructure.config.cache.CacheConfig;
import com.eshop.app.dto.security.UserPrincipalCache;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * Service orchestrating the two-tier cache strategy for resolved Keycloak identities. Uses local
 * Caffeine cache (L1) for sub-millisecond local reads, and falls back to distributed Redis cache
 * (L2) to prevent duplicate database writes across instances.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserPrincipalCacheService {

    private final Cache<String, UserPrincipalCache> userPrincipalCache; // L1 local cache
    private final CacheManager cacheManager; // Composite / L2 cache manager

    public UserPrincipalCache get(String keycloakId) {
        // 1. Try L1 (Caffeine)
        UserPrincipalCache cached = userPrincipalCache.getIfPresent(keycloakId);
        if (cached != null) {
            log.trace("L1 cache hit for user keycloakId={}", keycloakId);
            return cached;
        }

        // 2. Try L2 (Redis via Spring CacheManager)
        try {
            org.springframework.cache.Cache l2 = cacheManager.getCache(CacheConfig.USER_CACHE);
            if (l2 != null) {
                cached = l2.get(keycloakId, UserPrincipalCache.class);
                if (cached != null) {
                    log.debug("L2 cache hit for user keycloakId={}, populating L1", keycloakId);
                    userPrincipalCache.put(keycloakId, cached);
                    return cached;
                }
            }
        } catch (Exception e) {
            log.warn("Distributed L2 cache lookup failed: {}", e.getMessage());
        }

        return null;
    }

    public void put(String keycloakId, UserPrincipalCache principal) {
        // 1. Write to L1 (Caffeine)
        userPrincipalCache.put(keycloakId, principal);

        // 2. Write to L2 (Redis via Spring CacheManager)
        try {
            org.springframework.cache.Cache l2 = cacheManager.getCache(CacheConfig.USER_CACHE);
            if (l2 != null) {
                l2.put(keycloakId, principal);
                log.trace("Cached user keycloakId={} in L1 and L2 caches", keycloakId);
            }
        } catch (Exception e) {
            log.warn("Distributed L2 cache write failed: {}", e.getMessage());
        }
    }
}
