package com.eshop.app.user.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.eshop.app.core.exception.base.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UserSelfProtectionGuardTest {

    private SimpleMeterRegistry meterRegistry;
    private UserSelfProtectionGuard guard;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        guard = new UserSelfProtectionGuard(meterRegistry);
    }

    @Test
    void preventSelfOperation_throwsWhenIdsMatch() {
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () -> guard.preventSelfOperation(10L, 10L, SelfProtectedOperation.DELETE));
        assertEquals("Cannot delete your own account", ex.getMessage());
        assertEquals("USER_SELF_DELETE", ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void preventSelfOperation_doesNotThrowWhenIdsDiffer() {
        assertThatCode(() -> guard.preventSelfOperation(20L, 10L, SelfProtectedOperation.DELETE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperation_nullTargetId_doesNotThrow() {
        assertThatCode(() -> guard.preventSelfOperation(null, 10L, SelfProtectedOperation.DELETE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperation_nullCurrentUserId_doesNotThrow() {
        assertThatCode(() -> guard.preventSelfOperation(10L, null, SelfProtectedOperation.DELETE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperation_nullOperation_throwsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> guard.preventSelfOperation(10L, 10L, null));
    }

    @Test
    void preventSelfOperation_selfMatch_incrementsViolationMetric() {
        assertThrows(
                BusinessException.class,
                () -> guard.preventSelfOperation(10L, 10L, SelfProtectedOperation.DEACTIVATE));

        double count = meterRegistry.counter("security.self_protection.violation",
                "operation", "deactivate", "scope", "single").count();
        assertThat(count).isEqualTo(1.0);
    }

    @Test
    void preventSelfOperationInBulk_throwsWhenCurrentUserIdInList() {
        List<Long> ids = List.of(1L, 2L, 10L, 4L);
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                guard.preventSelfOperationInBulk(
                                        ids, 10L, SelfProtectedOperation.DEACTIVATE));
        assertEquals("Cannot deactivate your own account", ex.getMessage());
        assertEquals("USER_SELF_DEACTIVATE", ex.getErrorCode());
        assertEquals(HttpStatus.FORBIDDEN, ex.getHttpStatus());
    }

    @Test
    void preventSelfOperationInBulk_doesNotThrowWhenCurrentUserIdNotInList() {
        List<Long> ids = List.of(1L, 2L, 3L);
        assertThatCode(() ->
                        guard.preventSelfOperationInBulk(
                                ids, 10L, SelfProtectedOperation.DEACTIVATE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperationInBulk_nullCollection_doesNotThrow() {
        assertThatCode(() ->
                        guard.preventSelfOperationInBulk(null, 10L, SelfProtectedOperation.DEACTIVATE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperationInBulk_emptyCollection_doesNotThrow() {
        assertThatCode(() ->
                        guard.preventSelfOperationInBulk(
                                List.of(), 10L, SelfProtectedOperation.DEACTIVATE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperationInBulk_nullCurrentUserId_doesNotThrow() {
        assertThatCode(() ->
                        guard.preventSelfOperationInBulk(
                                List.of(1L, 2L), null, SelfProtectedOperation.DEACTIVATE))
                .doesNotThrowAnyException();
    }

    @Test
    void preventSelfOperationInBulk_nullOperation_throwsNullPointerException() {
        assertThatNullPointerException()
                .isThrownBy(() -> guard.preventSelfOperationInBulk(List.of(1L), 10L, null));
    }

    @Test
    void preventSelfOperationInBulk_nullElementInCollection_throwsBadRequest() {
        List<Long> idsWithNull = Arrays.asList(1L, null, 3L);
        BusinessException ex =
                assertThrows(
                        BusinessException.class,
                        () ->
                                guard.preventSelfOperationInBulk(
                                        idsWithNull, 10L, SelfProtectedOperation.DEACTIVATE));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
        assertEquals("INVALID_TARGET_IDS", ex.getErrorCode());
    }

    @Test
    void preventSelfOperationInBulk_selfMatch_incrementsBulkScopedViolationMetric() {
        assertThrows(
                BusinessException.class,
                () -> guard.preventSelfOperationInBulk(
                        List.of(1L, 10L, 3L), 10L, SelfProtectedOperation.ACTIVATE));

        double count = meterRegistry.counter("security.self_protection.violation",
                "operation", "activate", "scope", "bulk").count();
        assertThat(count).isEqualTo(1.0);
    }
}
