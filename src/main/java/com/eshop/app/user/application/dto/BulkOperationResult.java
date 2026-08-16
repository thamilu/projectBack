package com.eshop.app.user.application.dto;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** DTO record representing the outcome of a bulk user account state operation. */
public record BulkOperationResult(
        Map<String, Boolean> results,
        int totalRequested,
        int succeeded,
        int failed,
        List<String> failedIds,
        Instant completedAt,
        long durationMs) {
    /**
     * Creates a BulkOperationResult from a raw map of user ID -> success status.
     *
     * @param results The raw execution map
     * @return A constructed BulkOperationResult
     */
    public static BulkOperationResult of(Map<String, Boolean> results) {
        return of(results, System.currentTimeMillis());
    }

    /**
     * Creates a BulkOperationResult from a raw map and a start time timestamp to compute duration.
     *
     * @param results The raw execution map
     * @param startTimeMs The execution start epoch timestamp in milliseconds
     * @return A constructed BulkOperationResult
     */
    public static BulkOperationResult of(Map<String, Boolean> results, long startTimeMs) {
        List<String> failedIds =
                results.entrySet().stream()
                        .filter(e -> e != null && !e.getValue())
                        .map(entry -> entry.getKey())
                        .toList();
        int total = results.size();
        int failedCount = failedIds.size();
        int succeededCount = total - failedCount;
        long duration = System.currentTimeMillis() - startTimeMs;
        return new BulkOperationResult(
                Collections.unmodifiableMap(results),
                total,
                succeededCount,
                failedCount,
                Collections.unmodifiableList(failedIds),
                Instant.now(),
                duration);
    }

    /**
     * Returns an empty/no-op BulkOperationResult.
     *
     * @return A constructed empty BulkOperationResult
     */
    public static BulkOperationResult empty() {
        return new BulkOperationResult(
                Collections.emptyMap(), 0, 0, 0, Collections.emptyList(), Instant.now(), 0L);
    }
}
