package com.eshop.app.seed.core;

import java.util.List;

/**
 * Base interface for all entity seeders.
 * Follows Strategy Pattern for clean separation of concerns.
 * 
 * @param <T> Entity type to seed
 * @param <C> Context type (SeederContext for dependent data)
 */
public interface Seeder<T, C> {
    
    /**
     * Seed entities into the database.
     * 
     * @param context Context containing dependencies (e.g., saved users for shops)
     * @return List of saved entities
     */
    List<T> seed(C context);
    
    /**
     * Clean up existing data before seeding.
     * Uses deleteAllInBatch for optimal performance.
     */
    void cleanup();
    
    /**
     * Define execution order. Lower numbers execute first.
     * 
     * @return Priority order (1 = first, higher = later)
     */
    int order();
    
    /**
     * @return Human-readable seeder name for logging
     */
    String name();
}
