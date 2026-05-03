package com.eshop.app.seed.exception;

/**
 * Exception thrown when store seeding fails.
 */
public final class StoreSeedingException extends SeedingException {

    public StoreSeedingException(String message, Throwable cause) {
        super(message, cause, SeedPhase.STORE_SEEDING);
    }

    public StoreSeedingException(String message) {
        super(message, SeedPhase.STORE_SEEDING);
    }
}
