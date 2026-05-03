package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown during optimistic locking failures.
 * 
 * <p>This exception occurs when:
 * <ul>
 *   <li>Multiple concurrent updates to same entity</li>
 *   <li>Version mismatch in entity update</li>
 *   <li>Entity was modified by another transaction</li>
 * </ul>
 * 
 * <p>Recovery strategy: Retry operation with fresh entity state.
 * 
 * <p>HTTP Status: 409 CONFLICT
 * 
 * @since 1.0
 */
@Getter
public class OptimisticLockException extends RuntimeException {
    
    private final Long entityId;
    private final Class<?> entityType;
    private final Integer expectedVersion;
    private final Integer actualVersion;
    
    /**
     * Construct exception with entity details.
     * 
     * @param entityType the entity class
     * @param entityId the entity ID
     */
    public OptimisticLockException(Class<?> entityType, Long entityId) {
        super(String.format("Optimistic lock failed for %s with id %d. " +
            "Entity was modified by another transaction.", 
            entityType.getSimpleName(), entityId));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
    
    /**
     * Construct exception with version details.
     * 
     * @param entityType the entity class
     * @param entityId the entity ID
     * @param expectedVersion the expected version
     * @param actualVersion the actual version in database
     */
    public OptimisticLockException(Class<?> entityType, Long entityId, 
                                  Integer expectedVersion, Integer actualVersion) {
        super(String.format(
            "Optimistic lock failed for %s with id %d. " +
            "Expected version %d but found %d.", 
            entityType.getSimpleName(), entityId, expectedVersion, actualVersion));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }
    
    /**
     * Construct exception from JPA OptimisticLockException.
     * 
     * @param entityType the entity class
     * @param entityId the entity ID
     * @param cause the JPA exception
     */
    public OptimisticLockException(Class<?> entityType, Long entityId, Throwable cause) {
        super(String.format("Optimistic lock failed for %s with id %d", 
            entityType.getSimpleName(), entityId), cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = null;
        this.actualVersion = null;
    }
}
