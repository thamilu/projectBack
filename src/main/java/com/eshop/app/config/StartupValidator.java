package com.eshop.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validates critical dependencies at startup.
 */
@Component
public class StartupValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupValidator.class);

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final com.eshop.app.config.JwtProperties jwtProperties;

    public StartupValidator(Environment environment, JdbcTemplate jdbcTemplate, com.eshop.app.config.JwtProperties jwtProperties) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.jwtProperties = jwtProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateOnStartup() {
        log.info("=== Startup Validation ===");
        log.info("Active profiles: {}", (Object) environment.getActiveProfiles());
        validateDatabase();
        validateJwtConfiguration();
        log.info("=== Startup Validation Complete ===");
    }

    private void validateDatabase() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("✓ Database connection validated");
        } catch (Exception e) {
            log.error("✗ Database connection failed", e);
            throw new IllegalStateException("Database validation failed", e);
        }
    }

    private void validateJwtConfiguration() {
        if (jwtProperties.issuerUri() == null || jwtProperties.issuerUri().isBlank()) {
            throw new IllegalStateException("JWT issuer URI is not configured");
        }
        log.info("✓ JWT configuration validated (issuer: {})", jwtProperties.issuerUri());
    }
}
