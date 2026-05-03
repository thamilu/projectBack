package com.eshop.app.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;

/**
 * Managed Keycloak admin client bean.
 */
@Configuration
public class KeycloakAdminClientConfig {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClientConfig.class);

    private Keycloak keycloakAdminClient;

    @Bean
    @ConditionalOnProperty(name = "keycloak.admin.enabled", havingValue = "true", matchIfMissing = true)
    public Keycloak keycloakAdminClient(KeycloakConfig config) {
        log.info("Initializing Keycloak admin client for realm: {}", config.getRealm());

        this.keycloakAdminClient = KeycloakBuilder.builder()
            .serverUrl(config.getAuthServerUrl())
            .realm(config.getRealm())
            .clientId(config.getAdminClientId())
            .username(config.getAdminUsername())
            .password(config.getAdminPassword())
            .grantType("password")
            .build();

        try {
            String serverVersion = keycloakAdminClient.serverInfo().getInfo().getSystemInfo().getVersion();
            log.info("Keycloak admin client connected successfully. Server version: {}", serverVersion);
        } catch (Exception e) {
            log.error("Failed to verify Keycloak admin client connection", e);
            throw new IllegalStateException("Keycloak admin client initialization failed", e);
        }

        return keycloakAdminClient;
    }

    @PreDestroy
    public void cleanup() {
        if (keycloakAdminClient != null) {
            log.info("Closing Keycloak admin client");
            keycloakAdminClient.close();
        }
    }
}
