package com.eshop.app.core.infrastructure.config.security.oauth;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Orchestrates user identity synchronization with: - Distributed locking (prevents concurrent
 * duplicate syncs) - Retry with exponential backoff (handles transient DB failures) - Circuit
 * breaker (prevents thundering herd on DB outage) - Structured logging with MDC (enables
 * distributed tracing) - Graceful recovery with safe failure marking
 *
 * <p>Separated from the event listener to enable Spring AOP proxying of {@code @Retryable} and
 * {@code @CircuitBreaker} annotations.
 *
 * <p>Transactional contract: {@code processor.process()} owns its own {@code @Transactional}
 * boundary and commits (or rolls back) before returning control to this class — the distributed
 * lock (released in the {@code finally} block below, only after {@code process()} returns) is
 * therefore held for the entire processing + commit duration, not just the processing portion.
 * {@code processor.isAlreadySynced()} is a cache read (see {@link UserIdentitySyncProcessor}), so
 * idempotency state becomes visible to other threads no later than the lock release.
 *
 * <p><strong>Retry vs. Circuit Breaker interaction:</strong> {@code retryFor} on
 * {@code @Retryable} is a narrow allowlist ({@link TransientDataAccessException} only), so any
 * exception thrown by the circuit breaker itself when OPEN (Resilience4j's
 * {@code CallNotPermittedException}, a plain {@code RuntimeException}) can never match it and is
 * never retried, regardless of how the two aspects are nested. Retry attempts always resolve to a
 * single pass/fail outcome before the circuit breaker records it, so a transient failure is never
 * double-counted against the circuit breaker's failure-rate threshold across retry attempts.
 *
 * <p><strong>MDC handling:</strong> {@link #buildMdcContext} captures the calling thread's prior
 * MDC map and restores it via {@code MDC.clear()} + {@code MDC.setContextMap(previous)} on close.
 * {@code setContextMap} fully replaces the context map, so the preceding {@code clear()} is
 * redundant but harmless — this is not a context-corruption bug. It also matches this class's
 * caller, {@link UserIdentitySyncEventListener#handleUserIdentitySync}, which uses the identical
 * clear-then-restore idiom for the same reason (virtual thread pool reuse) — keep both in sync if
 * this is ever revisited.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserIdentitySyncOrchestrator {

    private static final String LOCK_SKIP_REASON = "CONCURRENT_LOCK_HELD";
    private static final String ALREADY_SYNCED_REASON = "IDEMPOTENCY_CHECK_PASSED";
    private static final String POST_LOCK_ALREADY_SYNCED_REASON = "POST_LOCK_IDEMPOTENCY_CHECK_PASSED";
    private static final int MAX_SAFE_MESSAGE_LENGTH = 200;

    private final UserIdentitySyncProcessor processor;
    private final UserSyncLockService lockService;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;

    /**
     * Functional interface representing a closeable MDC context. Prevents checked exception
     * propagation in try-with-resources.
     */
    @FunctionalInterface
    public interface MdcCloseable extends AutoCloseable {
        @Override
        void close();
    }

    /**
     * Executes identity synchronization with: - Idempotency guard (skips already-processed events)
     * - Distributed locking (prevents concurrent duplicate syncs) - Transient failure retry (up to
     * configured max attempts) - Circuit breaker (prevents DB thundering herd)
     *
     * <p>Non-retryable exceptions ({@link IllegalArgumentException}, {@link IllegalStateException})
     * propagate immediately to the caller.
     *
     * @param event The synchronization event (must not be null, keycloakId must not be blank)
     * @throws IllegalArgumentException if event or keycloakId is invalid
     */
    @CircuitBreaker(name = "userIdentitySync", fallbackMethod = "circuitBreakerFallback")
    @Retryable(
            retryFor = {TransientDataAccessException.class},
            noRetryFor = {IllegalArgumentException.class, IllegalStateException.class},
            maxAttemptsExpression = "${app.sync.retry.max-attempts:3}",
            backoff =
                    @Backoff(
                            delayExpression = "${app.sync.retry.backoff.delay:1000}",
                            multiplierExpression = "${app.sync.retry.backoff.multiplier:2}",
                            maxDelayExpression = "${app.sync.retry.backoff.max-delay:5000}"),
            listeners = "retryMetricsListener")
    public void syncWithRetry(UserIdentitySyncEvent event) {
        validateEvent(event);
        String keycloakId = event.getKeycloakId();

        try (MdcCloseable mdcCloseable = buildMdcContext(event)) {
            // Idempotency check before acquiring lock
            if (processor.isAlreadySynced(event)) {
                log.info(
                        "Skipping sync — event already processed. reason={}, keycloakId={},"
                                + " eventId={}",
                        ALREADY_SYNCED_REASON,
                        keycloakId,
                        event.getEventId());
                meterRegistry
                        .counter("user.sync.skipped", "reason", ALREADY_SYNCED_REASON)
                        .increment();
                return;
            }

            boolean lockAcquired = false;
            try {
                lockAcquired = lockService.tryLock(keycloakId);

                if (!lockAcquired) {
                    log.info(
                            "Skipping sync — concurrent processing detected. reason={},"
                                    + " keycloakId={}",
                            LOCK_SKIP_REASON,
                            keycloakId);
                    meterRegistry
                            .counter("user.sync.skipped", "reason", LOCK_SKIP_REASON)
                            .increment();
                    return;
                }

                // Post-lock idempotency re-check: another thread may have completed
                // processing (and released its lock) between our pre-lock check above and
                // acquiring this lock — the pre-lock check alone only prevents CONCURRENT
                // duplicate processing, not this sequential race.
                if (processor.isAlreadySynced(event)) {
                    log.info(
                            "Skipping sync — already processed by another thread while waiting"
                                    + " for lock. reason={}, keycloakId={}, eventId={}",
                            POST_LOCK_ALREADY_SYNCED_REASON,
                            keycloakId,
                            event.getEventId());
                    meterRegistry
                            .counter("user.sync.skipped", "reason", POST_LOCK_ALREADY_SYNCED_REASON)
                            .increment();
                    return;
                }

                log.debug("Lock acquired, beginning sync processing");
                processor.process(event);
                log.info("Sync completed successfully");
                meterRegistry.counter("user.sync.success").increment();

            } finally {
                if (lockAcquired) {
                    lockService.unlock(keycloakId);
                    log.debug("Lock released");
                }
            }
        }
    }

    /**
     * Circuit breaker fallback — invoked when the circuit is OPEN. Publishes a circuit-open event
     * for downstream alerting without processing.
     *
     * <p>Defensively null-checks {@code event}: Resilience4j binds the original method's argument
     * values for the fallback call, so a caller that invoked {@link #syncWithRetry} with a null
     * event (e.g. from a future refactor) would otherwise NPE here too.
     *
     * @param event The original sync event; may be null
     * @param ex The circuit breaker exception
     */
    public void circuitBreakerFallback(UserIdentitySyncEvent event, Exception ex) {
        String keycloakId = event != null ? event.getKeycloakId() : "unknown";
        log.warn(
                "Circuit breaker OPEN — userIdentitySync unavailable. keycloakId={}, exception={}",
                keycloakId,
                ex != null ? ex.getMessage() : "unknown");
        meterRegistry.counter("user.sync.circuit.open", "circuit", "userIdentitySync").increment();
        if (event != null) {
            eventPublisher.publishEvent(new UserSyncCircuitOpenEvent(event.getKeycloakId()));
        }
    }

    /**
     * Recovery handler — invoked after all retry attempts for {@link TransientDataAccessException}
     * are exhausted.
     *
     * @param e The persistent transient database exception triggering recovery
     * @param event The original sync event
     */
    @Recover
    public void recover(TransientDataAccessException e, UserIdentitySyncEvent event) {
        handlePermanentFailure(event, e, "transient_retries_exhausted");
    }

    /**
     * General recovery handler for any exception {@code processor.process()} throws that is
     * <em>not</em> a {@link TransientDataAccessException} — e.g. {@code
     * DataIntegrityViolationException} or any other unexpected {@link RuntimeException}. Spring
     * Retry's {@code retryFor} allowlist means these are never retried, but they still need the
     * same failure bookkeeping (cache mark + failure event) as the exhausted-retry path, since
     * {@link UserIdentitySyncEventListener#handleUserIdentitySync} has no exception handling of
     * its own around {@link #syncWithRetry} — without this handler, such exceptions would
     * otherwise vanish into Spring's default async-uncaught-exception logging with no failure
     * record and no alerting event.
     *
     * @param e The exception that caused the sync to fail
     * @param event The original sync event
     */
    @Recover
    public void recover(Exception e, UserIdentitySyncEvent event) {
        handlePermanentFailure(event, e, "non_retryable_exception");
    }

    /**
     * Shared permanent-failure handling for both {@code @Recover} methods above: logs, marks the
     * sync record FAILED, and publishes a failure event.
     *
     * <p>MDC context is best-effort here — a failure while building it (see
     * {@link #buildMdcContext}) must never prevent failure recording, since this is already the
     * last line of defense for a failed sync.
     */
    private void handlePermanentFailure(UserIdentitySyncEvent event, Exception e, String reason) {
        String keycloakId = event != null ? event.getKeycloakId() : "unknown";

        MdcCloseable mdcCloseable = null;
        try {
            if (event != null) {
                mdcCloseable = buildMdcContext(event);
            }
        } catch (Exception mdcEx) {
            log.warn("MDC context setup failed in recover() — continuing without it: {}",
                    mdcEx.getMessage());
        }

        try {
            log.error(
                    "Permanent sync failure. keycloakId={} reason={} exceptionType={}",
                    keycloakId, reason, e.getClass().getSimpleName(), e);
            meterRegistry.counter("user.sync.permanent.failure", "reason", reason).increment();

            if (event == null) {
                // Nothing to mark/publish without a keycloakId — this indicates a caller bug
                // (syncWithRetry invoked with a null event), already captured by the log line
                // and metric above.
                log.error("Cannot mark sync FAILED or publish failure event — event is null");
                return;
            }
            markFailedSafely(event, e);
            publishFailureEventSafely(event, e);
        } finally {
            if (mdcCloseable != null) {
                mdcCloseable.close();
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void validateEvent(UserIdentitySyncEvent event) {
        Objects.requireNonNull(event, "UserIdentitySyncEvent must not be null");
        if (!StringUtils.hasText(event.getKeycloakId())) {
            throw new IllegalArgumentException(
                    "keycloakId must not be null or blank in UserIdentitySyncEvent");
        }
        if (event.getEventId() == null) {
            throw new IllegalArgumentException(
                    "eventId must not be null in UserIdentitySyncEvent — required for tracing"
                            + " and idempotency correlation");
        }
    }

    private void markFailedSafely(UserIdentitySyncEvent event, Exception e) {
        try {
            processor.markFailed(event, e);
            log.info("Sync record marked as FAILED successfully");
        } catch (Exception markEx) {
            log.error(
                    "FATAL: Could not mark sync as FAILED for keycloakId={}. "
                            + "Manual intervention required. Data may be in inconsistent state.",
                    event.getKeycloakId(),
                    markEx);
            meterRegistry.counter("user.sync.mark.failed.error").increment();
        }
    }

    private void publishFailureEventSafely(UserIdentitySyncEvent event, Exception e) {
        try {
            String safeMessage = sanitizeExceptionMessage(e.getMessage());
            eventPublisher.publishEvent(
                    new UserSyncPermanentFailureEvent(event.getKeycloakId(), safeMessage));
            log.debug("UserSyncPermanentFailureEvent published");
        } catch (Exception publishEx) {
            log.error(
                    "Failed to publish UserSyncPermanentFailureEvent for keycloakId={}",
                    event.getKeycloakId(),
                    publishEx);
            meterRegistry.counter("user.sync.event.publish.error").increment();
        }
    }

    /**
     * Sanitizes exception messages before external exposure. Removes SQL fragments, JDBC URLs,
     * credentials, and schema references.
     */
    private String sanitizeExceptionMessage(String rawMessage) {
        if (!StringUtils.hasText(rawMessage)) {
            return "Transient database error - no details available";
        }
        String sanitized =
                rawMessage
                        .replaceAll(
                                "(?i)(password|passwd|pwd|secret|token)\\s*[=:]\\s*\\S+", "$1=[REDACTED]")
                        .replaceAll("(?i)jdbc:[^\\s;,)]+", "jdbc:[REDACTED]")
                        .replaceAll("(?i)(schema|table|column)\\s*[=:]\\s*\\S+", "$1=[REDACTED]")
                        // Hibernate/Spring Data wraps the failing SQL statement in "SQL [...]"
                        .replaceAll("(?i)SQL\\s*\\[[^\\]]{0,200}\\]", "SQL=[REDACTED]")
                        // Hostnames/IPs with an optional port, as seen in connection-failure
                        // messages (e.g. "Connection refused: db.internal.example.com:5432")
                        .replaceAll("\\b(?:\\d{1,3}\\.){3}\\d{1,3}(?::\\d+)?\\b", "[REDACTED]")
                        .replaceAll(
                                "(?i)\\b[a-z0-9-]+(?:\\.[a-z0-9-]+)+:\\d{2,5}\\b", "[REDACTED]");

        return sanitized.length() > MAX_SAFE_MESSAGE_LENGTH
                ? sanitized.substring(0, MAX_SAFE_MESSAGE_LENGTH) + "...[truncated]"
                : sanitized;
    }

    /**
     * Builds an MDC context with all relevant tracing fields. Returns AutoCloseable to support
     * try-with-resources pattern for guaranteed cleanup.
     */
    private MdcCloseable buildMdcContext(UserIdentitySyncEvent event) {
        Map<String, String> previousContext = MDC.getCopyOfContextMap();
        MDC.put("keycloakId", event.getKeycloakId());
        MDC.put("eventId", String.valueOf(event.getEventId()));
        MDC.put("correlationId", String.valueOf(event.getCorrelationId()));
        MDC.put("component", "UserIdentitySyncOrchestrator");
        return () -> {
            MDC.clear();
            if (previousContext != null) {
                MDC.setContextMap(previousContext);
            }
        };
    }
}
