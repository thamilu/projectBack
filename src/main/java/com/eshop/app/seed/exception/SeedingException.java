package com.eshop.app.seed.exception;

/**
 * Base exception for all seeding-related errors.
 * Sealed to enforce controlled exception hierarchy.
 */
public sealed class SeedingException extends RuntimeException 
        permits UserSeedingException, CatalogSeedingException, StoreSeedingException, InvalidSeedConfigException {
    
    private final SeedPhase phase;
    
    public SeedingException(String message, Throwable cause, SeedPhase phase) {
        super(message, cause);
        this.phase = phase;
    }
    
    public SeedingException(String message, SeedPhase phase) {
        super(message);
        this.phase = phase;
    }
    
    public SeedPhase getPhase() {
        return phase;
    }
    
    public enum SeedPhase {
        USER_SEEDING,
        CATEGORY_SEEDING,
        BRAND_SEEDING,
        TAG_SEEDING,
        STORE_SEEDING,
        PRODUCT_SEEDING,
        CART_SEEDING,
        CONFIGURATION,
        ORCHESTRATION
    }
}
