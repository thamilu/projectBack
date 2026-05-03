package com.eshop.app.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dashboard Cache Configuration
 * 
 * <p>Configures caching for dashboard endpoints to improve performance</p>
 * 
 * <p>Configured Caches:</p>
 * <ul>
 *   <li>adminDashboard - Admin dashboard overview</li>
 *   <li>sellerDashboard - Seller dashboard data</li>
 *   <li>customerDashboard - Customer dashboard data</li>
 *   <li>deliveryDashboard - Delivery agent dashboard data</li>
 *   <li>adminAnalytics - Admin analytics data</li>
 *   <li>sellerAnalytics - Seller analytics data</li>
 *   <li>quickStats - Quick statistics for all roles</li>
 * </ul>
 * 
 * @version 2.0
 * @since 2025-12-12
 */
@Configuration
@EnableCaching
public class DashboardCacheConfig {

    /**
     * Configure cache manager with all dashboard caches
     * <p>Uses simple in-memory cache for dashboard (no external dependency required)</p>
     * 
     * @return configured cache manager
     */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
    public CacheManager dashboardCacheManager() {
        return new ConcurrentMapCacheManager(
            "adminDashboard",
            "sellerDashboard",
            "customerDashboard",
            "deliveryDashboard",
            "adminAnalytics",
            "sellerAnalytics",
            "quickStats"
        );
    }
}
