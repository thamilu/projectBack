package com.eshop.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise cache configuration with Caffeine.
 * Optimized cache settings for different data types.
 *
 * Features:
 * - Individual TTL per cache
 * - Size-based eviction
 * - Statistics recording for monitoring
 * - Eviction logging
 *
 * Note: kept as a separate, enterprise-oriented cache configuration to
 * complement the main `CacheConfig` class.
 */
@Configuration
@EnableCaching
@Slf4j
class EnterpriseCacheConfig {

    // Cache names
    public static final String ADMIN_DASHBOARD = "adminDashboard";
    public static final String SELLER_DASHBOARD = "sellerDashboard";
    public static final String CUSTOMER_DASHBOARD = "customerDashboard";
    public static final String DELIVERY_DASHBOARD = "deliveryDashboard";
    public static final String ADMIN_STATISTICS = "adminStatistics";
    public static final String SELLER_STATISTICS = "sellerStatistics";
    public static final String PRODUCTS = "products";
    public static final String PRODUCTS_BY_SKU = "productsBySku";
    public static final String CATEGORIES = "categories";
    public static final String QUICK_STATS = "quickStats";
    public static final String ADMIN_ANALYTICS = "adminAnalytics";
    public static final String SELLER_ANALYTICS = "sellerAnalytics";
    
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager enterpriseCacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        List<CaffeineCache> caches = List.of(
                // Dashboard caches - 5 minutes TTL
                buildCache(ADMIN_DASHBOARD, 10, 300),
                buildCache(SELLER_DASHBOARD, 1000, 300),
                buildCache(CUSTOMER_DASHBOARD, 5000, 300),
                buildCache(DELIVERY_DASHBOARD, 500, 300),
                
                // Statistics caches - 5 minutes TTL
                buildCache(ADMIN_STATISTICS, 10, 300),
                buildCache(SELLER_STATISTICS, 1000, 300),
                buildCache(QUICK_STATS, 5000, 180),
                
                // Analytics caches - 10 minutes TTL
                buildCache(ADMIN_ANALYTICS, 100, 600),
                buildCache(SELLER_ANALYTICS, 1000, 600),
                
                // Product caches - 1 hour TTL
                buildCache(PRODUCTS, 10000, 3600),
                buildCache(PRODUCTS_BY_SKU, 10000, 3600),
                
                // Category cache - 2 hours TTL
                buildCache(CATEGORIES, 100, 7200)
        );
        
        cacheManager.setCaches(caches);
        return cacheManager;
    }
    
    /**
     * Builds a Caffeine cache with specified configuration.
     * 
     * @param name cache name
     * @param maxSize maximum number of entries
     * @param ttlSeconds time-to-live in seconds
     * @return configured Caffeine cache
     */
    private CaffeineCache buildCache(String name, int maxSize, int ttlSeconds) {
        return new CaffeineCache(
                name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttlSeconds, TimeUnit.SECONDS)
                        .recordStats() // Enable statistics for monitoring
                        .removalListener((Object key, Object value, RemovalCause cause) -> {
                            log.debug("Cache eviction - cache: {}, key: {}, cause: {}",
                                    name, key, cause);
                        })
                        .build()
        );
    }
}
