package com.eshop.app.core.lock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisDistributedLockAdapterTest {

    @Mock RedisTemplate<String, Object> redisTemplate;
    @Mock ValueOperations<String, Object> valueOperations;

    private RedisDistributedLockAdapter lockAdapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lockAdapter = new RedisDistributedLockAdapter(redisTemplate);
    }

    @Test
    void executeWithLock_runsTaskAndReleasesLockOnSuccess() {
        when(valueOperations.setIfAbsent(eq("lock:inventory:1"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenReturn(1L);

        String result =
                lockAdapter.executeWithLock(
                        "inventory:1", Duration.ofSeconds(1), Duration.ofSeconds(5), () -> "done");

        assertEquals("done", result);
        verify(redisTemplate).execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());
    }

    @Test
    void executeWithLock_releasesLockEvenWhenTaskThrows() {
        when(valueOperations.setIfAbsent(eq("lock:inventory:1"), any(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any()))
                .thenReturn(1L);

        assertThrows(
                LockAcquisitionException.class,
                () ->
                        lockAdapter.executeWithLock(
                                "inventory:1",
                                Duration.ofSeconds(1),
                                Duration.ofSeconds(5),
                                () -> {
                                    throw new IllegalStateException("boom");
                                }));

        verify(redisTemplate).execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());
    }

    @Test
    void executeWithLock_throwsWhenLockNeverAcquired() {
        when(valueOperations.setIfAbsent(eq("lock:inventory:1"), any(), any(Duration.class)))
                .thenReturn(false);

        assertThrows(
                LockAcquisitionException.class,
                () ->
                        lockAdapter.executeWithLock(
                                "inventory:1",
                                Duration.ofMillis(150),
                                Duration.ofSeconds(5),
                                () -> "should not run"));

        verify(redisTemplate, never()).execute(ArgumentMatchers.<RedisScript<Long>>any(), anyList(), any());
    }

    @Test
    void tryExecuteWithLock_returnsNullInsteadOfThrowingWhenLockUnavailable() {
        when(valueOperations.setIfAbsent(eq("lock:coupon:5:9"), any(), any(Duration.class)))
                .thenReturn(false);

        String result =
                lockAdapter.tryExecuteWithLock(
                        "coupon:5:9", Duration.ofMillis(150), Duration.ofSeconds(5), () -> "unreachable");

        assertEquals(null, result);
    }
}
