package com.eshop.app.seed.core;

import lombok.Data;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Result object tracking seeding execution metrics.
 */
@Data
public class SeedingResult {
    
    private boolean successful = true;
    private boolean skipped = false;
    private Instant startTime = Instant.now();
    private Instant endTime;
    private long durationMs;
    private Map<String, Integer> seederCounts = new HashMap<>();
    private String failureReason;
    
    public static SeedingResultBuilder builder() {
        return new SeedingResultBuilder();
    }
    
    public void addSeederResult(String seederName, int count) {
        seederCounts.put(seederName, count);
    }
    
    public int getUsersSeeded() {
        return seederCounts.getOrDefault("UserSeeder", 0);
    }
    
    public int getProductsSeeded() {
        return seederCounts.getOrDefault("ProductSeeder", 0);
    }
    
    public int getCategoriesSeeded() {
        return seederCounts.getOrDefault("CategorySeeder", 0);
    }
    
    @Override
    public String toString() {
        if (skipped) {
            return "Seeding skipped - database already populated";
        }
        return String.format("Seeding %s in %dms - %s",
            successful ? "succeeded" : "failed",
            durationMs,
            seederCounts);
    }
    
    /**
     * Custom builder for SeedingResult.
     */
    public static class SeedingResultBuilder {
        private final SeedingResult result = new SeedingResult();
        
        public SeedingResultBuilder successful(boolean successful) {
            result.successful = successful;
            return this;
        }
        
        public SeedingResultBuilder skipped(boolean skipped) {
            result.skipped = skipped;
            return this;
        }
        
        public SeedingResultBuilder startTime(Instant startTime) {
            result.startTime = startTime;
            return this;
        }
        
        public SeedingResultBuilder endTime(Instant endTime) {
            result.endTime = endTime;
            return this;
        }
        
        public SeedingResultBuilder durationMs(long durationMs) {
            result.durationMs = durationMs;
            return this;
        }
        
        public SeedingResultBuilder failureReason(String failureReason) {
            result.failureReason = failureReason;
            return this;
        }
        
        public SeedingResultBuilder addSeederResult(String seederName, int count) {
            result.seederCounts.put(seederName, count);
            return this;
        }
        
        public SeedingResult build() {
            return result;
        }
    }
}
