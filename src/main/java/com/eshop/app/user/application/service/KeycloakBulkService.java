package com.eshop.app.user.application.service;

import com.eshop.app.core.util.BatchUtils;
import com.eshop.app.user.application.dto.BulkOperationResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * Enterprise service managing high-concurrency bulk Keycloak user account updates. Leverages Java
 * Virtual Threads and monotonic timeout checks.
 */
@Component
@Slf4j
public class KeycloakBulkService {

    private final KeycloakServiceValidator validator;
    private final MaskingUtil maskingUtil;
    private final MeterRegistry meterRegistry;

    @Value("${keycloak.bulk.batch-size:50}")
    private int bulkBatchSize;

    @Value("${keycloak.bulk.timeout-seconds:30}")
    private int bulkTimeoutSeconds;

    public KeycloakBulkService(
            KeycloakServiceValidator validator,
            MaskingUtil maskingUtil,
            MeterRegistry meterRegistry) {
        this.validator = Objects.requireNonNull(validator, "validator");
        this.maskingUtil = Objects.requireNonNull(maskingUtil, "maskingUtil");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
    }

    public BulkOperationResult bulkSetUserEnabled(
            List<String> keycloakIds, boolean enabled, KeycloakAtomicOperations selfProxy) {
        if (CollectionUtils.isEmpty(keycloakIds)) {
            return BulkOperationResult.empty();
        }

        Map<Boolean, List<String>> fullPartition =
                keycloakIds.stream().collect(Collectors.partitioningBy(Objects::nonNull));
        List<String> nonNullIds = fullPartition.get(true);
        List<String> nullEntries = fullPartition.get(false);

        if (!nullEntries.isEmpty()) {
            log.warn("Bulk request contained [{}] null entries — excluded", nullEntries.size());
        }

        Map<Boolean, List<String>> partitioned =
                nonNullIds.stream().collect(Collectors.partitioningBy(validator::isValidUuid));
        List<String> validIds = partitioned.get(true);
        List<String> invalidIds = partitioned.get(false);

        Map<String, Boolean> results = new ConcurrentHashMap<>(keycloakIds.size());
        invalidIds.forEach(
                id -> {
                    log.warn(
                            "Invalid userId format in bulk request — pre-failing: [{}]",
                            maskingUtil.maskUserId(id));
                    results.put(id, false);
                });

        long startTimeMs = System.currentTimeMillis();
        if (validIds.isEmpty()) {
            log.warn("No valid user IDs in bulk request of [{}] total entries", keycloakIds.size());
            return BulkOperationResult.of(results, startTimeMs);
        }

        log.info(
                "Bulk {} for [{}] valid / [{}] invalid / [{}] null users",
                enabled ? "enable" : "disable",
                validIds.size(),
                invalidIds.size(),
                nullEntries.size());

        List<List<String>> batches = BatchUtils.partition(validIds, bulkBatchSize);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < batches.size(); i++) {
                processBatch(executor, batches.get(i), enabled, results, selfProxy);
            }
        }

        BulkOperationResult result = BulkOperationResult.of(results, startTimeMs);
        log.info(
                "Bulk complete: [{}/{}] succeeded, [{}] failed, duration=[{}ms]",
                result.succeeded(),
                result.totalRequested(),
                result.failed(),
                result.durationMs());

        meterRegistry
                .counter(
                        "keycloak.operation",
                        "operation",
                        "bulkSetUserEnabled",
                        "status",
                        result.failed() == 0 ? "success" : "partial_failure",
                        "role",
                        "none")
                .increment();

        return result;
    }

    private void processBatch(
            ExecutorService executor,
            List<String> batch,
            boolean enabled,
            Map<String, Boolean> results,
            KeycloakAtomicOperations selfProxy) {
        Map<String, String> callerMdc = MDC.getCopyOfContextMap();
        List<? extends Future<?>> futures =
                batch.stream()
                        .map(
                                userId ->
                                        executor.submit(
                                                () -> {
                                                    if (callerMdc != null) {
                                                        MDC.setContextMap(callerMdc);
                                                    }
                                                    try {
                                                        selfProxy.setUserEnabled(userId, enabled);
                                                        results.put(userId, true);
                                                    } catch (Exception e) {
                                                        log.warn(
                                                                "Failed to update user [{}]: {}",
                                                                maskingUtil.maskUserId(userId),
                                                                e.getMessage());
                                                        results.put(userId, false);
                                                    } finally {
                                                        MDC.clear();
                                                    }
                                                }))
                        .toList();
        awaitAll(futures);
    }

    private void awaitAll(List<? extends Future<?>> futures) {
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(bulkTimeoutSeconds);
        for (Future<?> future : futures) {
            try {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos <= 0) {
                    future.cancel(true);
                    continue;
                }
                future.get(remainingNanos, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                log.warn("Bulk task cancelled — exceeded timeout [{}s]", bulkTimeoutSeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Bulk operation interrupted — cancelling remaining tasks");
                futures.stream().filter(f -> !f.isDone()).forEach(f -> f.cancel(true));
                break;
            } catch (ExecutionException e) {
                // Task errors handled inside submitted Runnable
            }
        }
    }
}
