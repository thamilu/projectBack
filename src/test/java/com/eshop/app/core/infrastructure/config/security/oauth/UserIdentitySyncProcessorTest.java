package com.eshop.app.core.infrastructure.config.security.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eshop.app.dto.security.SyncStatus;
import com.eshop.app.dto.security.UserPrincipalCache;
import com.eshop.app.user.application.service.UserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

@ExtendWith(MockitoExtension.class)
class UserIdentitySyncProcessorTest {

    @Mock private UserService userService;
    @Mock private UserPrincipalCacheService cacheService;
    @Mock private UserPrincipalCacheFactory cacheFactory;

    private SimpleMeterRegistry meterRegistry;
    private UserIdentitySyncProcessor processor;
    private UserIdentitySyncEvent event;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        processor = new UserIdentitySyncProcessor(userService, cacheService, cacheFactory, meterRegistry);
        event = UserIdentitySyncEvent.builder()
                .keycloakId("kc-123")
                .email("user@example.com")
                .roles(Set.of("CUSTOMER"))
                .eventId(UUID.randomUUID())
                .correlationId("corr-1")
                .build();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void isAlreadySynced_returnsTrue_whenCacheStatusCompleted() {
        UserPrincipalCache cached = UserPrincipalCache.builder().syncStatus(SyncStatus.COMPLETED).build();
        when(cacheService.get("kc-123")).thenReturn(cached);

        assertThat(processor.isAlreadySynced(event)).isTrue();
    }

    @Test
    void isAlreadySynced_returnsFalse_whenCacheEntryMissing() {
        when(cacheService.get("kc-123")).thenReturn(null);

        assertThat(processor.isAlreadySynced(event)).isFalse();
    }

    @Test
    void isAlreadySynced_returnsFalse_whenCacheStatusNotCompleted() {
        UserPrincipalCache cached = UserPrincipalCache.builder().syncStatus(SyncStatus.PENDING).build();
        when(cacheService.get("kc-123")).thenReturn(cached);

        assertThat(processor.isAlreadySynced(event)).isFalse();
    }

    @Test
    void process_success_syncsUserAndRolesAndUpdatesCacheAsCompleted() {
        when(userService.syncUserFromKeycloak(any())).thenReturn(42L);
        when(cacheFactory.buildOrUpdate(any(), eq(event), eq(SyncStatus.COMPLETED), eq(42L)))
                .thenReturn(UserPrincipalCache.builder().userId(42L).syncStatus(SyncStatus.COMPLETED).build());

        processor.process(event);

        verify(userService).syncUserFromKeycloak(any());
        verify(userService).syncUserRoles(42L, event.getRoles());
        verify(cacheService).put(eq("kc-123"), any(UserPrincipalCache.class));
    }

    @Test
    void process_failure_rethrowsAndDoesNotUpdateCache() {
        when(userService.syncUserFromKeycloak(any())).thenThrow(new RuntimeException("db down"));

        assertThatThrownBy(() -> processor.process(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db down");

        verify(cacheService, never()).put(any(), any());
    }

    @Test
    void process_failure_recordsDynamicallyTaggedFailureTimer() {
        when(userService.syncUserFromKeycloak(any())).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(IllegalStateException.class);

        double count = meterRegistry.timer("user.identity.sync",
                "status", "failure",
                "exception", "IllegalStateException").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void process_preservesCallersPriorMdcValue_ratherThanDeletingIt() {
        // Simulates UserIdentitySyncOrchestrator having already set these MDC keys before
        // calling process() — regression test for the MDC.remove()-corrupts-caller-context bug.
        MDC.put("keycloakId", "kc-123");
        MDC.put("operation", "orchestrator-level-operation");
        when(userService.syncUserFromKeycloak(any())).thenReturn(1L);

        processor.process(event);

        assertThat(MDC.get("keycloakId")).isEqualTo("kc-123");
        assertThat(MDC.get("operation")).isEqualTo("orchestrator-level-operation");
    }

    @Test
    void process_removesMdcKeys_whenCallerHadNoPriorContext() {
        MDC.clear();
        when(userService.syncUserFromKeycloak(any())).thenReturn(1L);

        processor.process(event);

        assertThat(MDC.get("keycloakId")).isNull();
        assertThat(MDC.get("operation")).isNull();
    }

    @Test
    void process_restoresMdc_evenWhenProcessingThrows() {
        MDC.put("keycloakId", "outer-value");
        when(userService.syncUserFromKeycloak(any())).thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> processor.process(event)).isInstanceOf(RuntimeException.class);

        assertThat(MDC.get("keycloakId")).isEqualTo("outer-value");
    }

    @Test
    void markFailed_updatesCacheWithFailedStatusAndNullUserId() {
        RuntimeException cause = new RuntimeException("permanent failure");

        processor.markFailed(event, cause);

        ArgumentCaptor<SyncStatus> statusCaptor = ArgumentCaptor.forClass(SyncStatus.class);
        verify(cacheFactory).buildOrUpdate(any(), eq(event), statusCaptor.capture(), isNull());
        assertThat(statusCaptor.getValue()).isEqualTo(SyncStatus.FAILED);
    }

    @Test
    void markFailed_doesNotThrow_whenCauseIsNull() {
        assertThatCode(() -> processor.markFailed(event, null)).doesNotThrowAnyException();
    }

    @Test
    void markFailed_doesNotPropagate_whenCacheUpdateFails() {
        when(cacheFactory.buildOrUpdate(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("cache infra down"));

        assertThatCode(() -> processor.markFailed(event, new RuntimeException("original cause")))
                .doesNotThrowAnyException();
    }

    @Test
    void markFailed_incrementsCacheErrorCounter_whenCacheUpdateFails() {
        when(cacheFactory.buildOrUpdate(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("cache infra down"));

        processor.markFailed(event, new RuntimeException("original cause"));

        double count = meterRegistry.counter("user.identity.sync.mark_failed.cache_error").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void markFailed_doesNotIncrementCacheErrorCounter_whenCacheUpdateSucceeds() {
        processor.markFailed(event, new RuntimeException("original cause"));

        double count = meterRegistry.counter("user.identity.sync.mark_failed.cache_error").count();
        assertThat(count).isEqualTo(0.0);
    }
}
