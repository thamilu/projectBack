package com.eshop.app.seed.exception;

/**
 * Exception thrown when user seeding fails.
 */
public final class UserSeedingException extends SeedingException {
    
    public UserSeedingException(String message, Throwable cause) {
        super(message, cause, SeedPhase.USER_SEEDING);
    }
    
    public UserSeedingException(String message) {
        super(message, SeedPhase.USER_SEEDING);
    }
}
