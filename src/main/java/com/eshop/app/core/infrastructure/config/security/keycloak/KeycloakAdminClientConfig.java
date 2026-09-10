package com.eshop.app.core.infrastructure.config.security.keycloak;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PreDestroy;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

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
        log.info("Initializing Keycloak admin client for realm: {}", config.getAdminRealm());

        // eshop-admin-backend is a confidential service-account client (see
        // keycloak-import/eshop-admin-realm.json: publicClient=false,
        // directAccessGrantsEnabled=false, serviceAccountsEnabled=true with the
        // realm-management "realm-admin" role) living in the eshop-admin realm, not
        // the eshop realm. It must authenticate via client_credentials + client
        // secret, not a username/password "password" grant — Keycloak rejects
        // password grants for clients with direct access grants disabled (401).
        // Registers CustomKeycloakJacksonProvider so the admin REST client tolerates
        // fields the running Keycloak server sends that this keycloak-admin-client
        // version's DTOs don't yet know about (e.g. FeatureRepresentation.deprecated) —
        // Keycloak's server release cadence outpaces the admin-client artifact's.
        Client resteasyClient = ClientBuilder.newBuilder()
            .register(new CustomKeycloakJacksonProvider())
            .build();

        this.keycloakAdminClient = KeycloakBuilder.builder()
            .serverUrl(config.getAuthServerUrl())
            .realm(config.getAdminRealm())
            .clientId(config.getAdminClientId())
            .clientSecret(config.getAdminClientSecret())
            .grantType("client_credentials")
            .resteasyClient(resteasyClient)
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
