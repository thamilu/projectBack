package com.eshop.app.core.infrastructure.config.security.oauth;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Service providing distributed locking for user synchronization events. Falls back to in-memory
 * locking if Redis is unavailable or fails.
 */
@Component
@Slf4j
public class UserSyncLockService {

    private static final String LOCK_PREFIX = "eshop:user-sync-lock:";
    private static final long LOCK_TIMEOUT_SECONDS = 30L;

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, Boolean> inMemoryLocks = new ConcurrentHashMap<>();

    public UserSyncLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tries to acquire a lock for the given Keycloak user ID.
     *
     * @param keycloakId The Keycloak ID of the user to lock
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryLock(String keycloakId) {
        try {
            String lockKey = LOCK_PREFIX + keycloakId;
            Boolean acquired =
                    redisTemplate
                            .opsForValue()
                            .setIfAbsent(
                                    lockKey, "LOCKED", Duration.ofSeconds(LOCK_TIMEOUT_SECONDS));
            if (Boolean.TRUE.equals(acquired)) {
                return true;
            }
        } catch (Exception e) {
            log.warn(
                    "Redis lock acquisition failed for keycloakId={}, falling back to in-memory"
                            + " lock: {}",
                    keycloakId,
                    e.getMessage());
        }

        return tryInMemoryLock(keycloakId);
    }

    private boolean tryInMemoryLock(String keycloakId) {
        return inMemoryLocks.putIfAbsent(keycloakId, Boolean.TRUE) == null;
    }

    /**
     * Releases the lock for the given Keycloak user ID.
     *
     * @param keycloakId The Keycloak ID of the user to unlock
     */
    public void unlock(String keycloakId) {
        try {
            String lockKey = LOCK_PREFIX + keycloakId;
            redisTemplate.delete(lockKey);
        } catch (Exception e) {
            log.warn(
                    "Failed to release Redis lock for keycloakId={}: {}",
                    keycloakId,
                    e.getMessage());
        } finally {
            inMemoryLocks.remove(keycloakId);
        }
    }
}
