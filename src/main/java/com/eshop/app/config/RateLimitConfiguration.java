package com.eshop.app.config;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;

/**
 * Rate Limiting Configuration using Resilience4j
 * <p>
 * Provides tiered rate limiting to protect API endpoints from abuse:
 * <ul>
 *   <li><b>public:</b> 100 requests/minute - Public endpoints (product browsing)</li>
 *   <li><b>authenticated:</b> 500 requests/minute - Authenticated user endpoints</li>
 *   <li><b>premium:</b> 2000 requests/minute - Premium/seller accounts</li>
 *   <li><b>admin:</b> 5000 requests/minute - Admin operations</li>
 *   <li><b>analytics:</b> 20 requests/minute - Resource-intensive analytics</li>
 *   <li><b>payment:</b> 10 requests/minute - Payment processing endpoints</li>
 * </ul>
 */
@Configuration
@Slf4j
public class RateLimitConfiguration {
    
    /**
     * Rate limiter registry with predefined configurations
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Public endpoints - 100 requests per minute
        RateLimiterConfig publicConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(100)
            .timeoutDuration(Duration.ZERO) // Fail immediately if limit exceeded
            .build();
        
        // Authenticated users - 500 requests per minute
        RateLimiterConfig authenticatedConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(500)
            .timeoutDuration(Duration.ZERO)
            .build();
        
        // Premium/Seller accounts - 2000 requests per minute
        RateLimiterConfig premiumConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(2000)
            .timeoutDuration(Duration.ZERO)
            .build();
        
        // Admin operations - 5000 requests per minute
        RateLimiterConfig adminConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(5000)
            .timeoutDuration(Duration.ZERO)
            .build();
        
        // Analytics endpoints - 20 requests per minute (resource-intensive)
        RateLimiterConfig analyticsConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(20)
            .timeoutDuration(Duration.ofSeconds(5)) // Allow waiting up to 5 seconds
            .build();
        
        // Payment endpoints - 10 requests per minute
        RateLimiterConfig paymentConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .limitForPeriod(10)
            .timeoutDuration(Duration.ofSeconds(10)) // Allow retry for critical operations
            .build();
        
        // File upload - 30 requests per hour
        RateLimiterConfig uploadConfig = RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.ofHours(1))
            .limitForPeriod(30)
            .timeoutDuration(Duration.ZERO)
            .build();
        
        log.info("Initializing rate limiter registry with {} configurations", 7);
        
        return RateLimiterRegistry.of(Map.of(
            "public", publicConfig,
            "authenticated", authenticatedConfig,
            "premium", premiumConfig,
            "admin", adminConfig,
            "analytics", analyticsConfig,
            "payment", paymentConfig,
                "upload", uploadConfig,
                "login", RateLimiterConfig.custom()
                        .limitRefreshPeriod(Duration.ofMinutes(1))
                        .limitForPeriod(5)
                        .timeoutDuration(Duration.ZERO)
                        .build()
        ));
    }
}
