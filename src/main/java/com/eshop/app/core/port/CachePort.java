package com.eshop.app.core.port;

import java.time.Duration;
import java.util.Optional;

/**
 * [HARDEN] Cache Port — outbound port for distributed caching.
 *
 * Decouples the application from specific cache providers (Redis, Hazelcast, Caffeine).
 * Provides explicit governance over:
 * - TTL policies
 * - Key ownership
 * - Invalidation strategy
 *
 * Rules:
 * - All cache keys MUST be prefixed with module identifier (e.g., "catalog:products:")
 * - TTL must always be explicitly specified — no infinite caching
 * - Implementations must be thread-safe
 */
public interface CachePort {

    /**
     * Stores a value in cache with explicit TTL.
     *
     * @param key   the cache key (must be prefixed with module id)
     * @param value the value to cache
     * @param ttl   time-to-live duration
     * @param <V>   the value type
     */
    <V> void put(String key, V value, Duration ttl);

    /**
     * Retrieves a value from cache.
     *
     * @param key       the cache key
     * @param valueType the expected value class
     * @param <V>       the value type
     * @return Optional containing the cached value, or empty if missing/expired
     */
    <V> Optional<V> get(String key, Class<V> valueType);

    /**
     * Removes a specific key from cache.
     *
     * @param key the cache key to evict
     */
    void evict(String key);

    /**
     * Removes all keys matching the given prefix pattern.
     * Use with care on large caches.
     *
     * @param keyPattern prefix pattern (e.g., "catalog:products:*")
     */
    void evictByPattern(String keyPattern);

    /**
     * Checks whether a key currently exists and is not expired.
     *
     * @param key the cache key
     * @return true if a non-expired value exists
     */
    boolean exists(String key);
}
