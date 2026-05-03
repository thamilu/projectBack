package com.eshop.app.seed.exception;

/**
 * Exception thrown when catalog seeding (categories, brands, tags) fails.
 */
public final class CatalogSeedingException extends SeedingException {
    
    public CatalogSeedingException(String message, Throwable cause) {
        super(message, cause, SeedPhase.CATEGORY_SEEDING);
    }
    
    public CatalogSeedingException(String message) {
        super(message, SeedPhase.CATEGORY_SEEDING);
    }
}
