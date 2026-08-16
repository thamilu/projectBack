package com.eshop.app.core.infrastructure.config.security.oauth;

import com.eshop.app.dto.security.UserPrincipalCache;
import com.github.benmanes.caffeine.cache.Cache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Binds the local L1 User Principal Caffeine Cache to Micrometer for observability. Exposes metrics
 * for hit rate, miss rate, evictions, and cache size.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserPrincipalCacheMetrics {

    private final Cache<String, UserPrincipalCache> userPrincipalCache;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void registerMetrics() {
        try {
            CaffeineCacheMetrics.monitor(meterRegistry, userPrincipalCache, "user_principal");
            log.info("Registered User Principal Caffeine Cache (L1) metrics with Micrometer");
        } catch (Exception e) {
            log.error("Failed to register cache metrics: {}", e.getMessage(), e);
        }
    }
}
