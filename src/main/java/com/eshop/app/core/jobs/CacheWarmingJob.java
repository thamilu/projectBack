package com.eshop.app.core.jobs;
import com.eshop.app.core.infrastructure.config.security.oauth.SystemAuthenticationProvider;
import com.eshop.app.catalog.application.port.in.ProductUseCase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Background job for proactively warming critical cache entries.
 *
 * <p>Prevents cache stampede (thundering herd problem) by pre-loading hot
 * data before expiration. This job runs silently in the background with
 * zero impact on user-facing latency.
 *
 * <p>Part of the Enterprise Core Jobs Layer. Named {@code CacheWarmingJob}
 * (not Scheduler) to reflect the modular job architecture where each job
 * class owns a single operational responsibility.
 *
 * <p>Configuration:
 * <pre>
 *   cache.warming.enabled=true  # Enable/disable this job
 * </pre>
 *
 * @see com.eshop.app.core.cache.ResilientRedisCacheService
 * @since 2.0
 */
@Component
@ConditionalOnProperty(name = "cache.warming.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingJob {

    private final ProductUseCase productUseCase;
    private final CacheManager cacheManager;
    private final SystemAuthenticationProvider systemAuthProvider;

    /**
     * Warm featured products cache every 10 minutes.
     * Uses system auth to bypass @PreAuthorize restrictions.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 2)
    @SchedulerLock(name = "CacheWarmingJob_warmFeaturedProducts", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void warmFeaturedProducts() {
        log.debug("Warming featured products cache...");
        try {
            long start = System.currentTimeMillis();
            systemAuthProvider.runAsSystem(() -> {
                productUseCase.getFeaturedProducts(PageRequest.of(0, 20));
                return null;
            });
            log.info("Featured products cache warmed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Failed to warm featured products cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Warm top-selling products cache every 15 minutes.
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 3)
    @SchedulerLock(name = "CacheWarmingJob_warmTopSellingProducts", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void warmTopSellingProducts() {
        log.debug("Warming top-selling products cache...");
        try {
            long start = System.currentTimeMillis();
            systemAuthProvider.runAsSystem(() -> {
                productUseCase.getTopSellingProducts(10);
                return null;
            });
            log.info("Top-selling products cache warmed in {}ms", System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Failed to warm top-selling products cache: {}", e.getMessage(), e);
        }
    }

    /**
     * Clear stale cache entries daily at 3 AM to prevent memory bloat.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "CacheWarmingJob_clearStaleCache", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void clearStaleCache() {
        log.info("Clearing stale cache entries...");
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.debug("Cleared cache: {}", cacheName);
                }
            });
            log.info("All caches cleared successfully");
        } catch (Exception e) {
            log.error("Failed to clear stale caches: {}", e.getMessage(), e);
        }
    }

    /**
     * Log cache statistics every hour for operational observability.
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void logCacheStatistics() {
        log.info("Cache Statistics:");
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.info("  - {}: type={}", cacheName, cache.getNativeCache().getClass().getSimpleName());
            }
        });
    }
}
