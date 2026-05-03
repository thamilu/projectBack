package com.eshop.app.seed.config;

import com.eshop.app.seed.core.SeedOrchestrator;
import com.eshop.app.seed.core.SeedingResult;
import com.eshop.app.seed.exception.SeedingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class for database seeding.
 * Fixes CRITICAL transaction proxy bypass issue by calling orchestrator through Spring proxy.
 * 
 * Key fixes:
 * - Separate @Configuration from @Service to enable proper proxy
 * - @Transactional works on SeedOrchestrator because it's called externally
 * - Proper exception handling with fail-fast in dev
 * - Controlled by profile and property
 */
@Slf4j
@Configuration
@Profile({"dev", "test", "local"})
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeederConfig {
    
    private final SeedOrchestrator orchestrator;
    
    /**
     * CommandLineRunner that triggers seeding on application startup.
     * Calls orchestrator through Spring proxy to ensure transaction works.
     */
    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            try {
                log.info("=== Database Seeding Started ===");
                SeedingResult result = orchestrator.orchestrate();
                
                if (result.isSkipped()) {
                    log.info("=== Database Seeding Skipped (already seeded) ===");
                } else if (result.isSuccessful()) {
                    log.info("=== Database Seeding Completed Successfully ===");
                    log.info("Summary: {}", result.getSeederCounts());
                } else {
                    log.error("=== Database Seeding Failed ===");
                }
                
            } catch (SeedingException e) {
                log.error("Seeding failed at phase {}: {}", 
                    e.getPhase(), e.getMessage(), e);
                
                // In dev/test, fail fast to catch issues early
                if (isDevOrTestProfile()) {
                    throw new RuntimeException("Seeding failed - fix before proceeding", e);
                }
            } catch (Exception e) {
                log.error("Unexpected seeding error: {}", e.getMessage(), e);
                
                if (isDevOrTestProfile()) {
                    throw new RuntimeException("Seeding failed unexpectedly", e);
                }
            }
        };
    }
    
    private boolean isDevOrTestProfile() {
        // Could inject Environment if needed, but this is simpler for now
        return true; // Already restricted by @Profile annotation
    }
}
