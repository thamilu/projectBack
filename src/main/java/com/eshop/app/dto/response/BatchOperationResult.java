package com.eshop.app.dto.response;

import java.util.*;

/**
 * Generic result wrapper for batch operations.
 * Tracks successful and failed items with detailed error messages.
 *
 * @param <T> Type of items being processed
 * @author E-Shop Team
 * @since 2.0.0
 */
public record BatchOperationResult<T>(
    List<T> successful,
    List<BatchFailure> failures,
    int totalProcessed,
    int successCount,
    int failureCount
) {
    public boolean hasFailures() {
        return !failures.isEmpty();
    }
    
    public boolean isComplete() {
        return failureCount == 0;
    }
    
    public boolean isPartialSuccess() {
        return successCount > 0 && failureCount > 0;
    }
    
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }
    
    public static <T> BatchOperationResult<T> allSuccess(List<T> items) {
        return new BatchOperationResult<>(
            List.copyOf(items),
            List.of(),
            items.size(),
            items.size(),
            0
        );
    }
    
    public static class Builder<T> {
        private final List<T> successful = new ArrayList<>();
        private final List<BatchFailure> failures = new ArrayList<>();
        
        public Builder<T> addSuccess(T item) {
            successful.add(item);
            return this;
        }
        
        public Builder<T> addFailure(int index, String identifier, String error) {
            failures.add(new BatchFailure(index, identifier, error, null));
            return this;
        }
        
        public Builder<T> addFailure(int index, String identifier, String error, String errorCode) {
            failures.add(new BatchFailure(index, identifier, error, errorCode));
            return this;
        }
        
        public boolean hasFailures() {
            return !failures.isEmpty();
        }
        
        public BatchOperationResult<T> build() {
            return new BatchOperationResult<>(
                List.copyOf(successful),
                List.copyOf(failures),
                successful.size() + failures.size(),
                successful.size(),
                failures.size()
            );
        }
    }
}
