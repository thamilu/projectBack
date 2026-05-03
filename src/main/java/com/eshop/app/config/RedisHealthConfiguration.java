package com.eshop.app.config;

import io.lettuce.core.RedisConnectionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis Health Check Configuration - DEPRECATED
 * 
 * <p><b>REPLACED BY:</b> ResilientRedisCacheConfig with automatic failover.
 * 
 * <p>This configuration is no longer needed when using redis.resilient.mode=true.
 * The new resilient configuration provides:
 * <ul>
 *   <li>Automatic Redis → Caffeine fallback</li>
 *   <li>Fast failure with aggressive timeouts</li>
 *   <li>No health check failures</li>
 *   <li>Better performance through distributed caching</li>
 * </ul>
 * 
 * @deprecated Use {@link ResilientRedisCacheConfig} instead
 * @author EShop Infrastructure Team
 * @version 2.0
 * @since 2025-12-22
 */
@Configuration
@Slf4j
@Deprecated(since = "2.0", forRemoval = true)
@ConditionalOnProperty(name = "redis.resilient.mode", havingValue = "false", matchIfMissing = false)
public class RedisHealthConfiguration {

    @Value("${redis.enabled:false}")
    private boolean redisEnabled;

    /**
     * Simple Redis connection check when not using resilient mode.
     */
    @Bean
    @ConditionalOnProperty(name = "redis.enabled", havingValue = "true")
    @Deprecated(since = "2.0", forRemoval = true)
    public String redisConnectionStatus(RedisConnectionFactory connectionFactory) {
        try {
            connectionFactory.getConnection().ping();
            log.info("✓ Redis connection successful");
            return "connected";
        } catch (RedisConnectionException e) {
            log.warn("⚠ Redis connection failed: {} - Using Caffeine fallback", e.getMessage());
            return "disconnected";
        }
    }
}

