package com.eshop.app.user.infrastructure.event;

import com.eshop.app.user.application.port.out.IdentityProviderPort;
import com.eshop.app.user.domain.event.UserActivatedEvent;
import com.eshop.app.user.domain.event.UserDeactivatedEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event listener asynchronously dispatching Keycloak state updates. Runs on Java 21 Virtual Threads
 * and propagates tracing MDC.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private static final String MDC_KEY_KEYCLOAK_ID = "keycloakId";
    private static final String MDC_KEY_CORRELATION_ID = "correlationId";
    private static final String MDC_KEY_TRACE_ID = "traceId";
    private static final String MDC_UNKNOWN_VALUE = "unknown";

    private final IdentityProviderPort keycloakService;

    /**
     * Asynchronously handles user deactivation in Keycloak.
     *
     * @param event The deactivation event
     */
    @Async("virtualThreadExecutor")
    @EventListener
    public void handleUserDeactivated(UserDeactivatedEvent event) {
        if (event == null || event.getKeycloakId() == null) {
            return;
        }

        Map<String, String> parentMdc = populateTracingMdc(event.getKeycloakId(), event.getCorrelationId());
        try {
            log.info(
                    "Processing async user deactivation in Keycloak for keycloakId={}",
                    event.getKeycloakId());
            keycloakService.setUserEnabled(event.getKeycloakId(), false);
            log.info(
                    "Successfully deactivated user in Keycloak for keycloakId={}",
                    event.getKeycloakId());
        } catch (Exception e) {
            log.error(
                    "Failed to deactivate user in Keycloak for keycloakId={}",
                    event.getKeycloakId(),
                    e);
        } finally {
            restoreMdc(parentMdc);
        }
    }

    /**
     * Asynchronously handles user activation in Keycloak.
     *
     * @param event The activation event
     */
    @Async("virtualThreadExecutor")
    @EventListener
    public void handleUserActivated(UserActivatedEvent event) {
        if (event == null || event.getKeycloakId() == null) {
            return;
        }

        Map<String, String> parentMdc = populateTracingMdc(event.getKeycloakId(), event.getCorrelationId());
        try {
            log.info(
                    "Processing async user activation in Keycloak for keycloakId={}",
                    event.getKeycloakId());
            keycloakService.setUserEnabled(event.getKeycloakId(), true);
            log.info(
                    "Successfully activated user in Keycloak for keycloakId={}",
                    event.getKeycloakId());
        } catch (Exception e) {
            log.error(
                    "Failed to activate user in Keycloak for keycloakId={}",
                    event.getKeycloakId(),
                    e);
        } finally {
            restoreMdc(parentMdc);
        }
    }

    /**
     * Captures the current MDC context and populates tracing keys used for diagnostics.
     * Both {@code keycloakId} and {@code correlationId} are treated null-safely so no
     * literal {@code "null"} value is ever written into the logging context.
     *
     * @param keycloakId    the Keycloak user id (already validated non-null by callers)
     * @param correlationId the request correlation id; may be {@code null} if not propagated
     * @return the caller's prior MDC context map, to be restored via {@link #restoreMdc(Map)}
     */
    private Map<String, String> populateTracingMdc(String keycloakId, String correlationId) {
        Map<String, String> parentMdc = MDC.getCopyOfContextMap();
        String safeCorrelationId = correlationId != null ? correlationId : MDC_UNKNOWN_VALUE;
        MDC.put(MDC_KEY_KEYCLOAK_ID, keycloakId);
        MDC.put(MDC_KEY_CORRELATION_ID, safeCorrelationId);
        MDC.put(MDC_KEY_TRACE_ID, safeCorrelationId);
        return parentMdc;
    }

    private void restoreMdc(Map<String, String> parentMdc) {
        MDC.clear();
        if (parentMdc != null && !parentMdc.isEmpty()) {
            MDC.setContextMap(parentMdc);
        }
    }
}

