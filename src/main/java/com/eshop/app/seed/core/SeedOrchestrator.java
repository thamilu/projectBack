package com.eshop.app.seed.core;

import com.eshop.app.config.properties.SeedProperties;
import com.eshop.app.seed.validation.SeedPropertiesValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Orchestrates all seeders in proper order within a single transaction.
 * Fixes the transaction proxy bypass issue from original implementation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeedOrchestrator {
    
    private final List<Seeder<?, ?>> seeders;
    private final Environment environment;
    private final SeedPropertiesValidator validator;
    private final SeedProperties seedProperties;
    
    /**
     * Execute all seeders in order within a single transaction.
     * This method is called through Spring proxy, ensuring @Transactional works.
     * 
     * @return Result summary of seeding operation
     */
    @Transactional(rollbackFor = Exception.class)
    public SeedingResult orchestrate() {
        // Validate we're in appropriate environment
        validateEnvironment();
        
        // Validate configuration before proceeding
        validator.validate(seedProperties);
        
        // Check if seeding globally disabled (only checks validation/profile before
        // this)
        // Individual seeders will check their own 'enabled' flags.
        if (!seedProperties.isEnabled()) {
            log.info("Seeding globally disabled via app.seed.enabled=false");
            return SeedingResult.builder()
                .skipped(true)
                .durationMs(0)
                .build();
        }
        
        Instant startTime = Instant.now();
        SeedingResult.SeedingResultBuilder resultBuilder = SeedingResult.builder()
            .startTime(startTime);
        
        try {
            log.info("Starting database seeding...");
            
            SeederContext context = SeederContext.builder().build();
            
            // 1. Cleanup Phase - Execute in REVERSE order (Children first, then Parents)
            // This prevents Foreign Key constraint violations
            seeders.stream()
                    .sorted(Comparator.comparingInt((Seeder<?, ?> s) -> s.order()).reversed())
                    .forEach(seeder -> {
                        log.info("Cleaning up: {}", seeder.name());
                        seeder.cleanup();
                    });

            // 2. Seeding Phase - Execute in DEFINED order (Parents first, then Children)
            List<Seeder<?, ?>> orderedSeeders = seeders.stream()
                    .sorted(Comparator.comparingInt((Seeder<?, ?> s) -> s.order()))
                .toList();

            for (Seeder<?, ?> seeder : orderedSeeders) {
                log.info("Executing seeder: {}", seeder.name());

                // Seed new data
                @SuppressWarnings("unchecked")
                List<?> seeded = ((Seeder<Object, SeederContext>) seeder).seed(context);
                
                resultBuilder.addSeederResult(seeder.name(), seeded.size());
                log.info("Completed {}: {} entities", seeder.name(), seeded.size());
            }
            
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();
            
            resultBuilder
                .endTime(endTime)
                .durationMs(duration)
                .successful(true);
            
            SeedingResult result = resultBuilder.build();
            log.info("Database seeding completed successfully in {}ms: {}", 
                duration, result.getSeederCounts());
            
            return result;
            
        } catch (Exception e) {
            log.error("Database seeding failed", e);
            resultBuilder
                .successful(false)
                .failureReason(e.getMessage());
            
            throw e; // Rollback transaction
        }
    }
    

    
    /**
     * Validate that seeding is only run in dev/test/local profiles.
     * Prevents accidental production data wipe.
     */
    private void validateEnvironment() {
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        Set<String> allowedProfiles = Set.of("dev", "test", "local");
        
        if (Collections.disjoint(activeProfiles, allowedProfiles)) {
            throw new SecurityException(
                "Data seeding not allowed in profiles: " + activeProfiles + 
                ". Only allowed in: " + allowedProfiles);
        }
        
        log.debug("Environment validated for seeding: {}", activeProfiles);
    }
}
