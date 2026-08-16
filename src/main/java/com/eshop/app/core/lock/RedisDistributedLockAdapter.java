package com.eshop.app.core.lock;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis-backed implementation of {@link DistributedLockPort} using the standard {@code SET key
 * value NX PX} acquisition primitive and a Lua-scripted compare-and-delete for release (so a
 * caller can never release a lock it doesn't currently hold, e.g. after its own lease expired and
 * another caller acquired it in the meantime).
 *
 * <p>Uses the {@link RedisTemplate} already provided for caching rather than adding a Redisson
 * dependency — this port's contract (acquire-with-timeout, single compare-and-delete release)
 * doesn't need Redisson's fuller feature set (reentrant locks, pub/sub-based waiting, multi-node
 * Redlock), and the project standards favor the fewest new dependencies for a given need.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "spring.data.redis.repositories.enabled", havingValue = "true")
public class RedisDistributedLockAdapter implements DistributedLockPort {

    private static final String LOCK_KEY_PREFIX = "lock:";
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] then "
                            + "return redis.call('del', KEYS[1]) else return 0 end",
                    Long.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisDistributedLockAdapter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> task) {
        String token =
                acquire(lockKey, waitTime, leaseTime)
                        .orElseThrow(
                                () ->
                                        new LockAcquisitionException(
                                                lockKey,
                                                "Could not acquire lock '"
                                                        + lockKey
                                                        + "' within "
                                                        + waitTime));
        try {
            return task.call();
        } catch (LockAcquisitionException e) {
            throw e;
        } catch (Exception e) {
            throw new LockAcquisitionException(lockKey, "Task failed while holding lock '" + lockKey + "'", e);
        } finally {
            release(lockKey, token);
        }
    }

    @Override
    public <T> T tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> task) {
        try {
            return executeWithLock(lockKey, waitTime, leaseTime, task);
        } catch (LockAcquisitionException e) {
            log.debug("Lock '{}' not acquired within timeout, skipping task", lockKey);
            return null;
        }
    }

    private Optional<String> acquire(String lockKey, Duration waitTime, Duration leaseTime) {
        String redisKey = LOCK_KEY_PREFIX + lockKey;
        String token = UUID.randomUUID().toString();
        Instant deadline = Instant.now().plus(waitTime);

        while (true) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, token, leaseTime);
            if (Boolean.TRUE.equals(acquired)) {
                return Optional.of(token);
            }
            if (Instant.now().plus(POLL_INTERVAL).isAfter(deadline)) {
                return Optional.empty();
            }
            try {
                Thread.sleep(POLL_INTERVAL.toMillis());
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }
    }

    private void release(String lockKey, String token) {
        String redisKey = LOCK_KEY_PREFIX + lockKey;
        try {
            Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(redisKey), token);
            if (result == null || result == 0L) {
                log.warn(
                        "Lock '{}' was not released by this holder (already expired/reacquired by another caller)",
                        lockKey);
            }
        } catch (Exception e) {
            log.error("Error releasing lock '{}': {}", lockKey, e.getMessage(), e);
        }
    }
}
