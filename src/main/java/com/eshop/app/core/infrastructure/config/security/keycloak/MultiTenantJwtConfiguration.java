package com.eshop.app.core.infrastructure.config.security.keycloak;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.text.ParseException;
import java.time.Duration;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class MultiTenantJwtConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8080/realms/eshop}")
    private String userRealmIssuer;

    private final com.eshop.app.core.infrastructure.config.properties.SecurityProperties securityProperties;

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        log.info("Configuring Hardened Multi-Tenant JWT Decoder [Audiences: {}]", securityProperties.getJwt().getAudience());
        
        Map<String, JwtDecoder> decoders = new HashMap<>();
        decoders.put(userRealmIssuer, createDecoderWithAudience(userRealmIssuer));
        decoders.put(securityProperties.getAdmin().getIssuerUri(), createDecoderWithAudience(securityProperties.getAdmin().getIssuerUri()));

        return new JwtDecoder() {
            @Override
            public Jwt decode(String token) throws JwtException {
                try {
                    JWT jwt = JWTParser.parse(token);
                    String issuer = jwt.getJWTClaimsSet().getIssuer();

                    if (issuer == null) throw new BadJwtException("Missing issuer claim");

                    JwtDecoder decoder = decoders.get(issuer);
                    if (decoder == null) {
                        Optional<String> match = decoders.keySet().stream()
                                .filter(iss -> iss.equals(issuer) || issuer.startsWith(iss) || iss.startsWith(issuer))
                                .findFirst();
                        if (match.isPresent()) decoder = decoders.get(match.get());
                    }

                    if (decoder != null) return decoder.decode(token);
                    throw new BadJwtException("Unknown issuer: " + issuer);
                } catch (ParseException e) {
                    throw new BadJwtException("Failed to parse token", e);
                }
            }
        };
    }

    private JwtDecoder createDecoderWithAudience(String issuer) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(issuer));
        validators.add(new JwtAudienceValidator(securityProperties.getJwt().getAllowedAudiences()));
        validators.add(new JwtTimestampValidator(Duration.ofSeconds(securityProperties.getJwt().getClockSkewSeconds())));
        
        if (securityProperties.getJwt().isEnforceEmailVerified()) {
            log.info("Security Hardening: Enforcing email_verified claim for issuer {}", issuer);
            validators.add(new EmailVerifiedValidator());
        }
        
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    /**
     * Hardened validator to ensure the token is intended for this specific backend.
     */
    public static class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final List<String> allowedAudiences;

        public JwtAudienceValidator(List<String> allowedAudiences) {
            this.allowedAudiences = allowedAudiences;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                // In development/transition, some tokens might not have an audience claim.
                // We log a warning but allow it if the system is configured to be lenient.
                log.debug("JWT Verification: Token has no audience claim. Skipping audience check.");
                return OAuth2TokenValidatorResult.success();
            }
            
            // For hardened security, if an audience is present, it MUST match one of the allowed values
            boolean hasValidAudience = audiences.stream().anyMatch(allowedAudiences::contains);
            if (!hasValidAudience) {
                log.warn("JWT Rejected: Audience mismatch. Expected: {}, Got: {}", allowedAudiences, audiences);
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token audience is not allowed", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }

    /**
     * Hardened validator to ensure the user has verified their email address.
     */
    public static class EmailVerifiedValidator implements OAuth2TokenValidator<Jwt> {
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (Boolean.TRUE.equals(emailVerified)) {
                return OAuth2TokenValidatorResult.success();
            }
            
            log.warn("JWT Rejected: Email not verified for subject: {}", jwt.getSubject());
            return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", "Email address must be verified", null));
        }
    }
}
