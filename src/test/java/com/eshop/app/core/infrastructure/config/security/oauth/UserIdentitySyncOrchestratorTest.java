package com.eshop.app.core.infrastructure.config.security.oauth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.TransientDataAccessException;

@ExtendWith(MockitoExtension.class)
class UserIdentitySyncOrchestratorTest {

    @Mock private UserIdentitySyncProcessor processor;

    @Mock private UserSyncLockService lockService;

    @Mock private ApplicationEventPublisher eventPublisher;

    @Mock private MeterRegistry meterRegistry;

    @Mock private Counter counter;

    @Mock private SyncRetryProperties retryProperties;

    @InjectMocks private UserIdentitySyncOrchestrator orchestrator;

    private UserIdentitySyncEvent validEvent;

    @BeforeEach
    void setUp() {
        validEvent =
                UserIdentitySyncEvent.builder()
                        .keycloakId("kc-123")
                        .eventId(UUID.randomUUID())
                        .correlationId("corr-456")
                        .build();

        // Lenient stubbing for metrics registry counters
        lenient().when(meterRegistry.counter(anyString())).thenReturn(counter);
        lenient().when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    @Test
    @DisplayName("Should skip sync when event is already synced (idempotency)")
    void syncWithRetry_shouldSkip_whenAlreadySynced() {
        when(processor.isAlreadySynced(validEvent)).thenReturn(true);

        orchestrator.syncWithRetry(validEvent);

        verify(processor, never()).process(any());
        verifyNoInteractions(lockService);
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should skip sync when lock is already held by concurrent thread")
    void syncWithRetry_shouldSkip_whenLockAlreadyHeld() {
        when(processor.isAlreadySynced(validEvent)).thenReturn(false);
        when(lockService.tryLock("kc-123")).thenReturn(false);

        orchestrator.syncWithRetry(validEvent);

        verify(processor, never()).process(any());
        verify(lockService, never()).unlock(anyString());
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should release lock after successful processing")
    void syncWithRetry_shouldReleaseLock_afterSuccessfulProcessing() {
        when(processor.isAlreadySynced(validEvent)).thenReturn(false);
        when(lockService.tryLock("kc-123")).thenReturn(true);

        orchestrator.syncWithRetry(validEvent);

        verify(processor).process(validEvent);
        verify(lockService).unlock("kc-123");
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should release lock even when processing throws database exception")
    void syncWithRetry_shouldReleaseLock_evenWhenProcessingThrows() {
        when(processor.isAlreadySynced(validEvent)).thenReturn(false);
        when(lockService.tryLock("kc-123")).thenReturn(true);
        doThrow(new TransientDataAccessException("DB connection timeout") {})
                .when(processor)
                .process(validEvent);

        assertThrows(
                TransientDataAccessException.class, () -> orchestrator.syncWithRetry(validEvent));

        verify(lockService).unlock("kc-123");
    }

    @Test
    @DisplayName(
            "Should throw IllegalArgumentException for null event or blank keycloakId and not"
                    + " attempt lock")
    void syncWithRetry_shouldThrow_whenKeycloakIdIsBlank() {
        UserIdentitySyncEvent invalidEvent = UserIdentitySyncEvent.builder().keycloakId("").build();

        assertThrows(
                IllegalArgumentException.class, () -> orchestrator.syncWithRetry(invalidEvent));

        assertThrows(NullPointerException.class, () -> orchestrator.syncWithRetry(null));

        verifyNoInteractions(lockService, processor);
    }

    @Test
    @DisplayName(
            "Circuit breaker fallback should publish UserSyncCircuitOpenEvent and increment metric")
    void circuitBreakerFallback_shouldPublishEventAndMetric() {
        orchestrator.circuitBreakerFallback(validEvent, new RuntimeException("Circuit open"));

        verify(eventPublisher).publishEvent(any(UserSyncCircuitOpenEvent.class));
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should mark failed and publish event on recovery")
    void recover_shouldMarkFailedAndPublishEvent() {
        TransientDataAccessException ex = new TransientDataAccessException("timeout") {};

        orchestrator.recover(ex, validEvent);

        verify(processor).markFailed(validEvent, ex);
        verify(eventPublisher).publishEvent(any(UserSyncPermanentFailureEvent.class));
        verify(counter).increment();
    }

    @Test
    @DisplayName("Should still publish failure event even when markFailed throws in recovery")
    void recover_shouldPublishEvent_evenWhenMarkFailedThrows() {
        TransientDataAccessException ex = new TransientDataAccessException("timeout") {};
        doThrow(new RuntimeException("DB offline")).when(processor).markFailed(any(), any());

        orchestrator.recover(ex, validEvent);

        verify(processor).markFailed(validEvent, ex);
        verify(eventPublisher).publishEvent(any(UserSyncPermanentFailureEvent.class));
    }

    @Test
    @DisplayName("Should sanitize exception messages before publishing failure event")
    void recover_shouldSanitizeExceptionMessage() {
        TransientDataAccessException ex =
                new TransientDataAccessException(
                        "Failed connect to jdbc:postgresql://prod-db:5432/eshop with username=eshop"
                                + " and password=secret_pwd for schema=customers") {};

        orchestrator.recover(ex, validEvent);

        // capture the event published and verify sanitization
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        Object publishedEvent = eventCaptor.getValue();
        assertTrue(publishedEvent instanceof UserSyncPermanentFailureEvent);
        UserSyncPermanentFailureEvent failureEvent = (UserSyncPermanentFailureEvent) publishedEvent;
        String msg = failureEvent.getErrorMessage();
        assertFalse(msg.contains("secret_pwd"));
        assertFalse(msg.contains("prod-db"));
        assertFalse(msg.contains("customers"));
        assertTrue(msg.contains("[REDACTED]"));
    }

    @Test
    @DisplayName(
            "Should skip processing when another thread completed sync between pre-lock check"
                    + " and lock acquisition (post-lock idempotency re-check)")
    void syncWithRetry_shouldSkip_whenAlreadySyncedAfterLockAcquired() {
        // First call (pre-lock)sees not-yet-synced; second call (post-lock) sees a thread
        // that completed and released the lock in between.
        when(processor.isAlreadySynced(validEvent)).thenReturn(false, true);
        when(lockService.tryLock("kc-123")).thenReturn(true);

        orchestrator.syncWithRetry(validEvent);

        verify(processor, never()).process(any());
        verify(processor, times(2)).isAlreadySynced(validEvent);
        verify(lockService).unlock("kc-123");
    }

    @Test
    @DisplayName("General recover(Exception) handler should mark failed and publish event"
            + " for non-transient exceptions")
    void recover_generalHandler_shouldMarkFailedAndPublishEvent_forNonTransientException() {
        RuntimeException ex = new RuntimeException("constraint violation");

        orchestrator.recover(ex, validEvent);

        verify(processor).markFailed(validEvent, ex);
        verify(eventPublisher).publishEvent(any(UserSyncPermanentFailureEvent.class));
    }

    @Test
    @DisplayName("General recover(Exception) handler should not NPE and should skip"
            + " mark/publish when event is null")
    void recover_generalHandler_withNullEvent_doesNotThrow() {
        RuntimeException ex = new RuntimeException("boom");

        assertDoesNotThrow(() -> orchestrator.recover(ex, null));

        verify(processor, never()).markFailed(any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("circuitBreakerFallback with null event should not throw and should not"
            + " publish an event")
    void circuitBreakerFallback_withNullEvent_doesNotThrow() {
        assertDoesNotThrow(() ->
                orchestrator.circuitBreakerFallback(null, new RuntimeException("Circuit open")));

        verify(eventPublisher, never()).publishEvent(any());
        verify(counter).increment();
    }

    @Test
    @DisplayName("circuitBreakerFallback with null exception should not throw")
    void circuitBreakerFallback_withNullException_doesNotThrow() {
        assertDoesNotThrow(() -> orchestrator.circuitBreakerFallback(validEvent, null));

        verify(eventPublisher).publishEvent(any(UserSyncCircuitOpenEvent.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when eventId is null")
    void syncWithRetry_shouldThrow_whenEventIdIsNull() {
        UserIdentitySyncEvent eventWithoutId =
                UserIdentitySyncEvent.builder().keycloakId("kc-999").build();

        assertThrows(
                IllegalArgumentException.class, () -> orchestrator.syncWithRetry(eventWithoutId));

        verifyNoInteractions(lockService, processor);
    }
}
