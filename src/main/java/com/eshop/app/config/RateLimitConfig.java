package com.eshop.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rate limiting configuration using Resilience4j.
 * Protects endpoints from abuse and DoS attacks.
 */
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RateLimitConfig {

    private final RateLimiterRegistry rateLimiterRegistry;

    // Example: expose a specific RateLimiter bean for API usage
    @Bean
    public RateLimiter apiRateLimiter() {
        return rateLimiterRegistry.rateLimiter("api");
    }

    // Add more beans for other named rate limiters as needed
}
