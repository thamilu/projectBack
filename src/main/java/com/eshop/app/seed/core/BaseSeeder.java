package com.eshop.app.seed.core;

import com.eshop.app.seed.exception.SeedingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;

import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for all seeders to enforce DRY principles.
 * Centralizes logging, error handling, and basic metadata.
 */
@Slf4j
public abstract class BaseSeeder<T, C> implements Seeder<T, C> {

    @Override
    public final List<T> seed(C context) {
        log.info("🚀 Starting seeder: {}", name());
        try {
            List<T> result = doSeed(context);
            log.info("✅ {} completed: {} entities created", name(), result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (DataAccessException e) {
            log.error("❌ {} failed due to database error: {}", name(), e.getMessage(), e);
            throw new SeedingException("Database error in " + name() + ": " + e.getMessage(), e, SeedingException.SeedPhase.ORCHESTRATION);
        } catch (Exception e) {
            log.error("❌ {} failed: {}", name(), e.getMessage(), e);
            throw new SeedingException("Unexpected error in " + name() + ": " + e.getMessage(), e, SeedingException.SeedPhase.ORCHESTRATION);
        }
    }

    @Override
    public final void cleanup() {
        log.debug("🧹 Cleaning up: {}", name());
        try {
            doCleanup();
        } catch (Exception e) {
            log.warn("⚠️ Cleanup failed for {}: {}", name(), e.getMessage());
        }
    }

    /**
     * Actual seeding logic implemented by children.
     */
    protected abstract List<T> doSeed(C context);

    /**
     * Actual cleanup logic implemented by children.
     */
    protected abstract void doCleanup();

    @Override
    public abstract int order();

    @Override
    public String name() {
        return this.getClass().getSimpleName();
    }
}
