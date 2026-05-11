package com.eshop.app.config.properties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Hardened security configuration properties for Spring Boot 4.
 */
@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String audience = "eshop-backend,eshop-client,account";
        
        private long clockSkewSeconds = 300;
        
        private boolean enforceEmailVerified = false;

        public List<String> getAllowedAudiences() {
            return List.of(audience.split(","));
        }
    }

    @Getter
    @Setter
    public static class Admin {
        @NotBlank
        private String issuerUri = "http://localhost:8080/realms/master";
    }
}
