package com.eshop.app.core.lock;

/**
 * [HARDEN] Exception thrown when a distributed lock cannot be acquired within the wait timeout.
 *
 * Callers should handle this as a 503/retry scenario, not a 500 error.
 * Include the lockKey in the message for observability.
 */
public class LockAcquisitionException extends RuntimeException {

    private final String lockKey;

    public LockAcquisitionException(String lockKey, String message) {
        super(message);
        this.lockKey = lockKey;
    }

    public LockAcquisitionException(String lockKey, String message, Throwable cause) {
        super(message, cause);
        this.lockKey = lockKey;
    }

    public String getLockKey() {
        return lockKey;
    }
}
