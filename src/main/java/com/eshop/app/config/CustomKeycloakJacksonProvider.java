package com.eshop.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Custom Jackson Provider for Keycloak Admin Client.
 * 
 * [HARDEN] Resilience: Configures the ObjectMapper to ignore unknown properties
 * returned by the Keycloak server. This prevents the application from crashing
 * when Keycloak introduces new metadata fields (e.g., "multivalued") that are
 * not yet present in the project's keycloak-admin-client version.
 */
@Provider
public class CustomKeycloakJacksonProvider implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public CustomKeycloakJacksonProvider() {
        this.mapper = new ObjectMapper();
        
        // [HARDEN] Ignore unknown properties for forward compatibility with newer Keycloak versions
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // General enterprise standards for JSON mapping
        this.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.mapper.registerModule(new JavaTimeModule());
        
        // Keep original Keycloak default behaviors if any specific ones are known, 
        // but FAIL_ON_UNKNOWN_PROPERTIES=false is the critical fix here.
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }
}
