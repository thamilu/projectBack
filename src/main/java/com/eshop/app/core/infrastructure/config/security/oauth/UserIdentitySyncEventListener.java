package com.eshop.app.core.infrastructure.config.security.oauth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Thin asynchronous dispatcher for {@link UserIdentitySyncEvent}.
 *
 * <p>Responsibilities:
 *
 * <ul>
 *   <li>Validates event integrity before dispatch
 *   <li>Propagates MDC context to virtual thread for log correlation
 *   <li>Records dispatch metrics for observability
 *   <li>Delegates all sync logic to {@link UserIdentitySyncOrchestrator}
 * </ul>
 *
 * <p>Intentionally contains zero business logic — see SRP.
 */
@Component
@Slf4j
public class UserIdentitySyncEventListener {

    // ─── Constants ───────────────────────────────────────────────────────────

    private static final String MDC_KEY_KEYCLOAK_ID = "keycloakId";
    private static final String MDC_KEY_OPERATION = "asyncOperation";
    private static final String OPERATION_NAME = "user-identity-sync";
    private static final String METRIC_DISPATCH_TOTAL = "user.identity.sync.dispatched";
    private static final String METRIC_DROPPED_TOTAL = "user.identity.sync.dropped";

    // ─── Dependencies ────────────────────────────────────────────────────────

    private final UserIdentitySyncOrchestrator orchestrator;
    private final Counter dispatchCounter;
    private final Counter droppedCounter;

    // ─── Constructor ─────────────────────────────────────────────────────────

    public UserIdentitySyncEventListener(
            UserIdentitySyncOrchestrator orchestrator, MeterRegistry meterRegistry) {
        this.orchestrator = orchestrator;
        // Pre-register counters once at construction — not on hot path
        this.dispatchCounter =
                Counter.builder(METRIC_DISPATCH_TOTAL)
                        .description("Total user identity sync events dispatched")
                        .register(meterRegistry);
        this.droppedCounter =
                Counter.builder(METRIC_DROPPED_TOTAL)
                        .description(
                                "Total user identity sync events dropped due to invalid payload")
                        .register(meterRegistry);
    }

    // ─── Event Handler ───────────────────────────────────────────────────────

    /**
     * Asynchronously dispatches user identity synchronization upon receiving a {@link
     * UserIdentitySyncEvent}. Executes on Java 21 Virtual Threads via the configured {@code
     * virtualThreadExecutor}.
     *
     * <p>Triggered when a user authenticates via Keycloak and their local profile is either absent
     * or marked as requiring re-synchronization.
     *
     * @param event the sync event carrying Keycloak user identity details; must not be null
     */
    @Async("virtualThreadExecutor")
    @EventListener
    public void handleUserIdentitySync(UserIdentitySyncEvent event) {
        // ── Guard: validate event integrity before any processing ──
        if (!isValidEvent(event)) {
            droppedCounter.increment();
            log.error(
                    "Dropping malformed UserIdentitySyncEvent — " + "event={} keycloakId={}",
                    event,
                    event != null ? event.getKeycloakId() : "null");
            return;
        }

        // ── Propagate MDC context to virtual thread ──
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();
        MDC.put(MDC_KEY_KEYCLOAK_ID, event.getKeycloakId());
        MDC.put(MDC_KEY_OPERATION, OPERATION_NAME);

        try {
            dispatchCounter.increment();
            log.info("Dispatching async identity sync");
            orchestrator.syncWithRetry(event);
        } finally {
            // Always restore MDC — virtual threads are reused in thread pools
            restoreMdc(parentMdc);
        }
    }

    // ─── Private Helpers ─────────────────────────────────────────────────────

    /**
     * Validates that the event and its primary key are non-null. Additional field validation is the
     * responsibility of the orchestrator/processor.
     */
    private boolean isValidEvent(UserIdentitySyncEvent event) {
        return event != null && event.getKeycloakId() != null && !event.getKeycloakId().isBlank();
    }

    /**
     * Restores MDC to the parent thread's state, or clears if no parent context existed. Critical
     * for virtual thread pools to prevent context leakage between tasks.
     */
    private void restoreMdc(Map<String, String> parentMdc) {
        MDC.clear();
        if (parentMdc != null && !parentMdc.isEmpty()) {
            MDC.setContextMap(parentMdc);
        }
    }
}
