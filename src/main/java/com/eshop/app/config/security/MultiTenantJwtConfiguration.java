package com.eshop.app.config.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.security.oauth2.jwt.*;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class MultiTenantJwtConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String userRealmIssuer;

    @Value("${app.security.admin.issuer-uri}")
    private String adminRealmIssuer;

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        log.info("Configuring Multi-Tenant JWT Decoder");
        log.info(" - User Realm: {}", userRealmIssuer);
        log.info(" - Admin Realm: {}", adminRealmIssuer);

        Map<String, JwtDecoder> decoders = new HashMap<>();
        decoders.put(userRealmIssuer, NimbusJwtDecoder.withIssuerLocation(userRealmIssuer).build());
        decoders.put(adminRealmIssuer, NimbusJwtDecoder.withIssuerLocation(adminRealmIssuer).build());

        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                try {
                    // Parse token without validation to find issuer
                    JWT jwt = JWTParser.parse(token);
                    JWTClaimsSet claims = jwt.getJWTClaimsSet();
                    String issuer = claims.getIssuer();

                    if (issuer == null) {
                        throw new BadJwtException("Token is missing issuer claim");
                    }

                    JwtDecoder decoder = decoders.get(issuer);

                    // Fallback or precise match logic
                    if (decoder == null) {
                        // Try to match by checking if the configured issuer is a prefix or equivalent
                        // Keycloak sometimes returns localhost vs 127.0.0.1 discrepancies
                        Optional<String> match = decoders.keySet().stream()
                                .filter(iss -> iss.equals(issuer) || issuer.startsWith(iss) || iss.startsWith(issuer))
                                .findFirst();

                        if (match.isPresent()) {
                            decoder = decoders.get(match.get());
                            log.debug("Fuzzy matched issuer '{}' to configured realm '{}'", issuer, match.get());
                        }
                    }

                    if (decoder != null) {
                        try {
                            log.debug("Delegating token validation to decoder for issuer: {}", issuer);
                            return decoder.decode(token);
                        } catch (JwtException e) {
                            log.error("Token validation failed for issuer '{}'. configured_issuers={}. Error: {}",
                                    issuer, decoders.keySet(), e.getMessage());
                            throw e;
                        }
                    } else {
                        log.warn("Unknown issuer: {}. Allowed realms: {}", issuer, decoders.keySet());
                        throw new BadJwtException("Unknown issuer: " + issuer);
                    }
                } catch (ParseException e) {
                    throw new BadJwtException("Failed to parse token", e);
                }
            }
        };
    }
}
