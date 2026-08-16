package com.eshop.app.core.infrastructure.config.security.oauth;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

/**
 * Custom retry listener that increments Micrometer metrics on retry attempts and logs warnings.
 * Registered as a Spring bean and referenced by name in @Retryable(listeners = ...) — the bean
 * name Spring assigns by default ("retryMetricsListener", from the class name) must match that
 * reference exactly.
 */
@Component
@Slf4j
public class RetryMetricsListener implements RetryListener {

    private final Counter retryCounter;

    public RetryMetricsListener(MeterRegistry meterRegistry) {
        this.retryCounter =
                Counter.builder("user.identity.sync.retries")
                        .description("Number of user identity sync retry attempts")
                        .register(meterRegistry);
    }

    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        retryCounter.increment();
        log.warn(
                "Retry attempt {} for user identity sync due to: {}",
                context.getRetryCount(),
                throwable.getMessage());
    }
}
