package com.eshop.app.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Development-only permissive JwtDecoder to avoid strict JWT validation when using Swagger/UI locally.
 * This decoder accepts any Bearer token and produces a Jwt with basic claims.
 * 
 * DISABLED: Use real Keycloak tokens instead
 */
@Configuration
@Profile("dev-mock-jwt-only")  // Changed from "dev" to disable this mock decoder
public class DevJwtDecoderConfig {

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            Instant now = Instant.now();
            Map<String, Object> headers = new HashMap<>();
            headers.put("alg", "none");

                Map<String, Object> claims = new HashMap<>();
                claims.put("sub", "dev-user");
                claims.put("preferred_username", "dev-user");
                claims.put("roles", List.of("ADMIN"));

                // Also include Keycloak-style realm_access for configs that extract roles there
                Map<String, Object> realmAccess = new HashMap<>();
                realmAccess.put("roles", List.of("ADMIN"));
                claims.put("realm_access", realmAccess);

                Instant expiresAt = now.plus(365, ChronoUnit.DAYS);

                return Jwt.withTokenValue(token)
                    .headers(h -> h.putAll(headers))
                    .claims(c -> c.putAll(claims))
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .build();
        };
    }
}
