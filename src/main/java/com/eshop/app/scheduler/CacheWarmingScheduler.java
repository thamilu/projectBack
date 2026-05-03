package com.eshop.app.scheduler;

import com.eshop.app.config.SystemAuthenticationProvider;
import com.eshop.app.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * MEDIUM-001 FIX: Cache Warming Scheduler
 * 
 * <p>Proactively refreshes critical cache entries before expiration to prevent
 * cache stampede (thundering herd problem) where multiple requests simultaneously
 * query the database when a popular cache entry expires.
 * 
 * <h2>Cache Stampede Problem:</h2>
 * <pre>
 * Time T: Cache expires for popular product
 * T+1ms: 100 concurrent requests all miss cache
 * T+2ms: 100 database queries executed simultaneously
 * Result: Database overload, increased latency
 * </pre>
 * 
 * <h2>Solution:</h2>
 * <ul>
 *   <li>Refresh cache entries before expiration</li>
 *   <li>Stagger refresh times to prevent simultaneous queries</li>
 *   <li>Use background scheduler for zero user-facing latency</li>
 * </ul>
 * 
 * <h2>Scheduled Refreshes:</h2>
 * <ul>
 *   <li>Featured products: Every 10 minutes</li>
 *   <li>Top-selling products: Every 15 minutes</li>
 *   <li>Dashboard statistics: Every 5 minutes</li>
 * </ul>
 * 
 * <h2>CRITICAL-003 FIX:</h2>
 * <p>Uses {@link SystemAuthenticationProvider} to execute with system privileges,
 * allowing calls to @PreAuthorize annotated service methods.
 * 
 * <h2>Configuration:</h2>
 * <pre>
 * # Enable/disable cache warming
 * cache.warming.enabled=true
 * </pre>
 * 
 * @author EShop Performance Team
 * @version 2.0
 * @since 2025-12-20
 */
@Component
@ConditionalOnProperty(name = "cache.warming.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingScheduler {
    
    private final ProductService productService;
    private final CacheManager cacheManager;
    private final SystemAuthenticationProvider systemAuthProvider;
    
    /**
     * Warm featured products cache.
     * Runs every 10 minutes to keep hot data fresh.
     * 
     * Cron: Every 10 minutes
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES, initialDelay = 2)
    public void warmFeaturedProductsCache() {
        log.debug("Warming featured products cache...");
        
        try {
            long start = System.currentTimeMillis();
            
            // CRITICAL-003 FIX: Execute with system privileges
            systemAuthProvider.runAsSystem(() -> {
                // Pre-load featured products (most accessed on homepage)
                productService.getFeaturedProducts(PageRequest.of(0, 20));
                return null;
            });
            
            long duration = System.currentTimeMillis() - start;
            log.info("✓ Featured products cache warmed in {}ms", duration);
            
        } catch (Exception e) {
            log.error("Failed to warm featured products cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Warm top-selling products cache.
     * Runs every 15 minutes as this data changes less frequently.
     * 
     * Cron: Every 15 minutes
     */
    @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 3)
    public void warmTopSellingProductsCache() {
        log.debug("Warming top-selling products cache...");
        
        try {
            long start = System.currentTimeMillis();
            
            // CRITICAL-003 FIX: Execute with system privileges
            systemAuthProvider.runAsSystem(() -> {
                // Pre-load top sellers (service expects a limit, not a Pageable)
                productService.getTopSellingProducts(10);
                return null;
            });
            
            long duration = System.currentTimeMillis() - start;
            log.info("✓ Top-selling products cache warmed in {}ms", duration);
            
        } catch (Exception e) {
            log.error("Failed to warm top-selling products cache: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Clear stale cache entries.
     * Runs daily at 3 AM to prevent memory bloat from unused entries.
     * 
     * Cron: Daily at 3:00 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void clearStaleCache() {
        log.info("Clearing stale cache entries...");
        
        try {
            // Clear all caches (they will be rebuilt on demand)
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    log.debug("Cleared cache: {}", cacheName);
                }
            });
            
            log.info("✓ All caches cleared successfully");
            
        } catch (Exception e) {
            log.error("Failed to clear stale caches: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Log cache statistics.
     * Runs every hour for operational visibility.
     * 
     * Cron: Every hour
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void logCacheStatistics() {
        log.info("📊 Cache Statistics:");
        
        cacheManager.getCacheNames().forEach(cacheName -> {
            var cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Object nativeCache = cache.getNativeCache();
                log.info("  - {}: Native cache type: {}", 
                    cacheName, 
                    nativeCache.getClass().getSimpleName()
                );
                
                // For Caffeine cache, could extract detailed stats here
                // if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache caffeinCache) {
                //     com.github.benmanes.caffeine.cache.stats.CacheStats stats = caffeineCache.stats();
                //     log.info("    Hit rate: {}, Evictions: {}", stats.hitRate(), stats.evictionCount());
                // }
            }
        });
    }
}
