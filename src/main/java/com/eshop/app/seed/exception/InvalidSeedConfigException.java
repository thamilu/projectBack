package com.eshop.app.seed.exception;

/**
 * Exception thrown when seed configuration is invalid.
 */
public final class InvalidSeedConfigException extends SeedingException {
    
    public InvalidSeedConfigException(String message, Throwable cause) {
        super(message, cause, SeedPhase.CONFIGURATION);
    }
    
    public InvalidSeedConfigException(String message) {
        super(message, SeedPhase.CONFIGURATION);
    }
}
