package com.eshop.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Placeholder Keycloak health component used when Actuator is not present.
 */
@Component
public class KeycloakHealthIndicator {
    private static final Logger log = LoggerFactory.getLogger(KeycloakHealthIndicator.class);

    public void pingPlaceholder() {
        log.debug("KeycloakHealthIndicator placeholder active");
    }
}
