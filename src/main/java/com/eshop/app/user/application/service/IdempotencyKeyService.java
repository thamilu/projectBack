package com.eshop.app.user.application.service;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

/**
 * Service to manage API idempotency keys using Redis storage (TTL 24 hours). Prevents
 * double-submission of mutating bulk operations using atomic check-and-claim strategy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyService {

    private final StringRedisTemplate redisTemplate;
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:bulk:";

    /**
     * Checks if this is the first execution for the given key and reserves it atomically.
     *
     * @param key The idempotency key
     * @param operationType The type of bulk operation
     * @return IdempotencyResult representing the execution claim outcome
     */
    public IdempotencyResult checkAndClaim(String key, String operationType) {
        if (key == null || key.isBlank()) {
            return IdempotencyResult.firstExecution();
        }

        String redisKey = KEY_PREFIX + operationType + ":" + key;
        try {
            // Atomic GET then SET NX with TTL in a single Lua script
            String luaScript =
                    """
                    local existing = redis.call('GET', KEYS[1])
                    if existing then
                        return existing
                    end
                    redis.call('SET', KEYS[1], 'PROCESSING', 'EX', ARGV[1])
                    return nil
                    """;

            String existing =
                    redisTemplate.execute(
                            RedisScript.of(luaScript, String.class),
                            List.of(redisKey),
                            String.valueOf(IDEMPOTENCY_TTL.toSeconds()));

            if (existing == null) {
                return IdempotencyResult.firstExecution();
            }
            if ("PROCESSING".equals(existing)) {
                return IdempotencyResult.processing();
            }
            return IdempotencyResult.cached(existing);
        } catch (Exception e) {
            log.warn(
                    "Redis operations failed during checkAndClaim for key={}: {}. Defaulting to"
                            + " firstExecution.",
                    key,
                    e.getMessage());
            return IdempotencyResult.firstExecution();
        }
    }

    /**
     * Stores the final JSON result of the operation in Redis.
     *
     * @param key The idempotency key
     * @param operationType The type of bulk operation
     * @param serializedResult The serialized string output
     */
    public void storeResult(String key, String operationType, String serializedResult) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = KEY_PREFIX + operationType + ":" + key;
        try {
            redisTemplate.opsForValue().set(redisKey, serializedResult, IDEMPOTENCY_TTL);
        } catch (Exception e) {
            log.warn(
                    "Failed to store idempotency result in Redis for key={}: {}",
                    key,
                    e.getMessage());
        }
    }

    /**
     * Clears the idempotency key reservation (e.g. upon failure of primary transaction).
     *
     * @param key The idempotency key
     * @param operationType The type of bulk operation
     */
    public void clearKey(String key, String operationType) {
        if (key == null || key.isBlank()) {
            return;
        }
        String redisKey = KEY_PREFIX + operationType + ":" + key;
        try {
            redisTemplate.delete(redisKey);
            log.info("Cleared idempotency key={} for op={}", key, operationType);
        } catch (Exception e) {
            log.warn("Failed to clear idempotency key {} in Redis: {}", key, e.getMessage());
        }
    }

    /** Sealed interface representing the atomic idempotency claim outcomes. */
    public sealed interface IdempotencyResult
            permits IdempotencyResult.FirstExecution,
                    IdempotencyResult.Processing,
                    IdempotencyResult.CachedResult {

        static IdempotencyResult firstExecution() {
            return new FirstExecution();
        }

        static IdempotencyResult processing() {
            return new Processing();
        }

        static IdempotencyResult cached(String v) {
            return new CachedResult(v);
        }

        record FirstExecution() implements IdempotencyResult {}

        record Processing() implements IdempotencyResult {}

        record CachedResult(String serializedValue) implements IdempotencyResult {}
    }
}
