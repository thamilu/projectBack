package com.eshop.app.service.cache;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Resilient Redis cache service with circuit breaker and fallback.
 */
@Service
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true")
public class ResilientRedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(ResilientRedisCacheService.class);
    private static final String REDIS_CB = "redisCircuitBreaker";

    private final RedisTemplate<String, Object> redisTemplate;

    public ResilientRedisCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @CircuitBreaker(name = REDIS_CB, fallbackMethod = "getFallback")
    @Retry(name = "redisRetry")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(type.cast(value));
    }

    @CircuitBreaker(name = REDIS_CB, fallbackMethod = "setFallback")
    @Retry(name = "redisRetry")
    public void set(String key, Object value, Duration ttl) {
        if (ttl == null) {
            redisTemplate.opsForValue().set(key, value);
        } else {
            redisTemplate.opsForValue().set(key, value, ttl);
        }
    }

    @CircuitBreaker(name = REDIS_CB, fallbackMethod = "deleteFallback")
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    // Fallback methods (invoked by Resilience4j via reflection)
    @SuppressWarnings("unused")
    private <T> Optional<T> getFallback(String key, Class<T> type, Throwable t) {
        log.warn("Redis GET fallback triggered for key={}, error={}", key, t.getMessage());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private void setFallback(String key, Object value, Duration ttl, Throwable t) {
        log.warn("Redis SET fallback triggered for key={}, error={}", key, t.getMessage());
        // Optionally queue for retry or use local cache
    }

    @SuppressWarnings("unused")
    private Boolean deleteFallback(String key, Throwable t) {
        log.warn("Redis DELETE fallback triggered for key={}, error={}", key, t.getMessage());
        return false;
    }
}
