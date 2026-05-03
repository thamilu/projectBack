package com.eshop.app.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Placeholder configuration kept for compatibility.
 * The previous implementation referenced a Springdoc class not present
 * in the project's dependency version; use the OpenAPI proxy controller
 * instead to expose admin documentation in non-prod environments.
 */
@Configuration
@Slf4j
@Profile("!prod")
public class SwaggerUiUrlOverrideConfig {

    @PostConstruct
    public void init() {
        log.info("SwaggerUiUrlOverrideConfig loaded (no-op placeholder)");
    }
}
