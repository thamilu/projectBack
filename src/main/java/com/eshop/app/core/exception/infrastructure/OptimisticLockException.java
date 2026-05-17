package com.eshop.app.core.exception.infrastructure;

import com.eshop.app.core.exception.base.InfrastructureException;
import lombok.Getter;

@Getter
public class OptimisticLockException extends InfrastructureException {
    
    private final Long entityId;
    private final Class<?> entityType;
    private final Integer expectedVersion;
    private final Integer actualVersion;
    
    public OptimisticLockException(Class<?> entityType, Long entityId) {
        super(String.format("Optimistic lock failed for %s with id %d. " +
            "Entity was modified by another transaction.", 
            entityType.getSimpleName(), entityId), "OPTIMISTIC_LOCK_FAILED");
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
    
    public OptimisticLockException(Class<?> entityType, Long entityId, 
                                  Integer expectedVersion, Integer actualVersion) {
        super(String.format(
            "Optimistic lock failed for %s with id %d. " +
            "Expected version %d but found %d.", 
            entityType.getSimpleName(), entityId, expectedVersion, actualVersion), "OPTIMISTIC_LOCK_FAILED");
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    public OptimisticLockException(Class<?> entityType, Long entityId, Throwable cause) {
        super(String.format("Optimistic lock failed for %s with id %d", 
            entityType.getSimpleName(), entityId), "OPTIMISTIC_LOCK_FAILED", cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
}
