package com.eshop.app.core.infrastructure.config.security.oauth;

import com.eshop.app.dto.security.SyncStatus;
import com.eshop.app.dto.security.UserPrincipalCache;
import com.eshop.app.user.application.command.UserSyncCommand;
import com.eshop.app.user.application.service.UserService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles the transactional business logic and state synchronization of Keycloak identities.
 * Pre-registers Micrometer timers to prevent registration overhead on the hot path.
 *
 * <p><strong>Caller contract:</strong> {@link #process} must only be invoked while the caller
 * holds the per-{@code keycloakId} distributed lock (see {@code UserSyncLockService}, acquired by
 * {@code UserIdentitySyncOrchestrator} before calling this method). {@link #updateCache}'s
 * read-modify-write on the cache is not itself atomic; it relies entirely on that external lock
 * to serialize concurrent updates for the same {@code keycloakId} — do not call {@link #process}
 * or {@link #markFailed} directly without holding that lock.
 *
 * <p><strong>Cache failure isolation:</strong> {@code UserPrincipalCacheService.get}/{@code .put}
 * already catch and log (not rethrow) failures from the L2 (Redis) tier internally, degrading to
 * L1 (Caffeine)-only operation. {@link #updateCache} is therefore not expected to throw from
 * cache-infrastructure issues — only from a genuine programming defect (e.g. in {@code
 * UserPrincipalCacheFactory}), which should legitimately roll back {@link #process}'s transaction
 * rather than be masked.
 */
@Component
@Slf4j
public class UserIdentitySyncProcessor {

    private final UserService userService;
    private final UserPrincipalCacheService cacheService;
    private final UserPrincipalCacheFactory cacheFactory;
    private final MeterRegistry meterRegistry;
    private final Timer successTimer;
    private final Counter markFailedCacheErrorCounter;

    public UserIdentitySyncProcessor(
            UserService userService,
            UserPrincipalCacheService cacheService,
            UserPrincipalCacheFactory cacheFactory,
            MeterRegistry meterRegistry) {
        this.userService = userService;
        this.cacheService = cacheService;
        this.cacheFactory = cacheFactory;
        this.meterRegistry = meterRegistry;

        // Pre-register timers/counters once during construction — avoids per-call
        // registration overhead on the hot path.
        this.successTimer =
                Timer.builder("user.identity.sync")
                        .tag("status", "success")
                        .description("Successful user identity sync duration")
                        .register(meterRegistry);

        // Failure timers are registered dynamically per exception type in process()'s catch
        // block (see the "exception" tag there) rather than pre-registered here like
        // successTimer — the aggregate failure count/duration is still queryable by summing
        // across the "exception" tag, so no separate untagged series is needed.

        this.markFailedCacheErrorCounter =
                Counter.builder("user.identity.sync.mark_failed.cache_error")
                        .description(
                                "Times updating the FAILED status in cache itself failed during"
                                        + " recovery — these failures are otherwise invisible,"
                                        + " since markFailed() intentionally never rethrows")
                        .register(meterRegistry);
    }

    /**
     * Checks if the event has already been synchronized successfully by querying the L1/L2 cache.
     *
     * @param event The UserIdentitySyncEvent details
     * @return true if the cache entry indicates COMPLETED status, false otherwise
     */
    public boolean isAlreadySynced(UserIdentitySyncEvent event) {
        UserPrincipalCache cached = cacheService.get(event.getKeycloakId());
        return cached != null && cached.getSyncStatus() == SyncStatus.COMPLETED;
    }

    /**
     * Synchronizes the user details and roles within a single database transaction. Updates the
     * L1/L2 caches as part of the same method call (see class Javadoc for why this is safe: cache
     * failures don't propagate from the cache service, and concurrent calls for the same
     * keycloakId are already serialized by the caller's distributed lock).
     *
     * <p>Uses {@link Propagation#REQUIRES_NEW} — not just an explicit {@code Isolation} — so the
     * "owns its own transaction boundary" contract in this Javadoc is actually enforced by Spring
     * rather than merely documented. With the default {@code REQUIRED} propagation, if this method
     * were ever called from within an existing transaction (e.g. a future caller wraps it in its
     * own {@code @Transactional}), it would silently join that transaction instead, and the
     * {@code Isolation.READ_COMMITTED} here would be silently ignored (isolation only takes effect
     * when a transaction actually starts).
     *
     * @param event The UserIdentitySyncEvent details
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.READ_COMMITTED)
    public void process(UserIdentitySyncEvent event) {
        String keycloakId = event.getKeycloakId();
        Timer.Sample sample = Timer.start();

        // Save/restore rather than unconditional remove: the caller (UserIdentitySyncOrchestrator)
        // already sets "keycloakId" in MDC before invoking this method and keeps logging after it
        // returns — an unconditional MDC.remove() here would delete the caller's own MDC entry,
        // not just this method's temporary overwrite of it.
        String previousKeycloakId = MDC.get("keycloakId");
        String previousOperation = MDC.get("operation");
        MDC.put("keycloakId", keycloakId);
        MDC.put("operation", "user-identity-sync");

        try {
            log.info("Starting background user identity sync");

            Long userId = userService.syncUserFromKeycloak(UserSyncCommand.from(event));
            userService.syncUserRoles(userId, event.getRoles());

            updateCache(event, userId, SyncStatus.COMPLETED);

            log.info("User identity sync completed successfully, local userId={}", userId);
            sample.stop(successTimer);

        } catch (Exception e) {
            log.error("User identity sync failed: {}", e.getMessage(), e);
            // Exception-type tag is registered dynamically (not pre-registered like
            // successTimer/failureTimer) since the exception type varies per call — Micrometer
            // caches timers by name+tags internally, so repeated calls with the same exception
            // type reuse the same Timer instance rather than re-registering. Cardinality is
            // naturally bounded: exception types come from this method's own fixed set of
            // downstream calls, not from unbounded user input.
            sample.stop(meterRegistry.timer("user.identity.sync",
                    "status", "failure",
                    "exception", e.getClass().getSimpleName()));
            throw e;
        } finally {
            restoreMdc("keycloakId", previousKeycloakId);
            restoreMdc("operation", previousOperation);
        }
    }

    /** Restores a single MDC key to its prior value, or removes it if there was none. */
    private void restoreMdc(String key, String previousValue) {
        if (previousValue != null) {
            MDC.put(key, previousValue);
        } else {
            MDC.remove(key);
        }
    }

    /**
     * Marks the cache entry as FAILED. Executed from the recovery path.
     *
     * <p>NOTE: processor.markFailed() updates the cache independent of any database transaction.
     * {@code userId} is intentionally passed as {@code null} to {@link #updateCache} —
     * {@code UserPrincipalCacheFactory.buildOrUpdate} only overwrites the cached {@code userId}
     * when a non-null value is supplied, so any previously-resolved {@code userId} on an existing
     * cache entry is preserved rather than clobbered by this failure marker.
     *
     * <p>Never rethrows: this is the last line of defense in the recovery path, called from
     * {@code UserIdentitySyncOrchestrator}'s {@code @Recover} handlers, where an exception
     * escaping here would itself be swallowed by Spring Retry's recovery machinery with no
     * further handling. A failure to update the cache here is recorded via {@link
     * #markFailedCacheErrorCounter} — otherwise it would be invisible to metrics/alerting
     * entirely, since it's caught and only logged.
     *
     * @param event The original UserIdentitySyncEvent details
     * @param cause The exception that caused the permanent failure — logged here since this is
     *              often the only place it's recorded with full context (keycloakId + stack trace)
     */
    public void markFailed(UserIdentitySyncEvent event, Exception cause) {
        log.error(
                "Marking sync as permanently FAILED for keycloakId={}: {}",
                event.getKeycloakId(),
                cause != null ? cause.getMessage() : "unknown cause",
                cause);
        try {
            updateCache(event, null, SyncStatus.FAILED);
        } catch (Exception cacheEx) {
            log.error(
                    "Failed to mark sync status as FAILED in cache for keycloakId={}: {}",
                    event.getKeycloakId(),
                    cacheEx.getMessage(),
                    cacheEx);
            markFailedCacheErrorCounter.increment();
        }
    }

    private void updateCache(UserIdentitySyncEvent event, Long userId, SyncStatus status) {
        String keycloakId = event.getKeycloakId();
        UserPrincipalCache existing = cacheService.get(keycloakId);
        UserPrincipalCache updated = cacheFactory.buildOrUpdate(existing, event, status, userId);
        cacheService.put(keycloakId, updated);
    }
}
