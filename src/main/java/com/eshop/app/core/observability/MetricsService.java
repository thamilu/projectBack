package com.eshop.app.core.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Centralized metrics facade for the E-Shop platform.
 *
 * <p>Wraps Micrometer's {@link MeterRegistry} to provide named, consistent
 * metric recording across all domain services. This prevents ad-hoc metric
 * naming drift and ensures all metrics follow a unified naming convention
 * (e.g., {@code eshop.order.created}, {@code eshop.product.view}).
 *
 * <p>Part of the Enterprise Core Observability Layer.
 *
 * <p>Usage:
 * <pre>
 * {@code
 * metricsService.incrementCounter("order.created", "status", "SUCCESS");
 * metricsService.recordTimer("product.search.duration", durationMs, "source", "catalog");
 * }
 * </pre>
 *
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private static final String METRIC_PREFIX = "eshop.";
    private final MeterRegistry meterRegistry;

    /**
     * Increments a named counter with optional key-value tags.
     *
     * @param name   short metric name (e.g., "order.created"); prefix is added automatically
     * @param tags   alternating key-value tag pairs (e.g., "status", "SUCCESS")
     */
    public void incrementCounter(String name, String... tags) {
        try {
            meterRegistry.counter(METRIC_PREFIX + name, buildTags(tags)).increment();
        } catch (Exception e) {
            log.warn("Failed to increment counter [{}]: {}", name, e.getMessage());
        }
    }

    /**
     * Records a timer observation for latency tracking.
     *
     * @param name       short metric name (e.g., "product.search.duration")
     * @param durationMs observed duration in milliseconds
     * @param tags        alternating key-value tag pairs
     */
    public void recordTimer(String name, long durationMs, String... tags) {
        try {
            meterRegistry.timer(METRIC_PREFIX + name, buildTags(tags))
                    .record(durationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Failed to record timer [{}]: {}", name, e.getMessage());
        }
    }

    /**
     * Records a gauge (current value snapshot, e.g., active session count).
     *
     * @param name  short metric name
     * @param value current value
     * @param tags  alternating key-value tag pairs
     */
    public void recordGauge(String name, double value, String... tags) {
        try {
            meterRegistry.gauge(METRIC_PREFIX + name, buildTags(tags), value);
        } catch (Exception e) {
            log.warn("Failed to record gauge [{}]: {}", name, e.getMessage());
        }
    }

    // ── Internal Helpers ──────────────────────────────────────────────────────

    private List<Tag> buildTags(String... keyValuePairs) {
        if (keyValuePairs == null || keyValuePairs.length == 0) return List.of();
        if (keyValuePairs.length % 2 != 0) {
            log.warn("Metric tags must be provided as key-value pairs. Odd number detected.");
            return List.of();
        }
        Tag[] tags = new Tag[keyValuePairs.length / 2];
        for (int i = 0; i < keyValuePairs.length; i += 2) {
            tags[i / 2] = Tag.of(keyValuePairs[i], keyValuePairs[i + 1]);
        }
        return List.of(tags);
    }
}
