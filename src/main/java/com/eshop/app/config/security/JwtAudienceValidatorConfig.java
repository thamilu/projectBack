package com.eshop.app.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import java.time.Duration;
import java.util.List;

@Configuration
@Profile("!dev")
@ConditionalOnProperty(prefix = "spring.security.oauth2.resourceserver.jwt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class JwtAudienceValidatorConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${app.security.jwt.audience:eshop-backend}")
    private String allowedAudience;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        OAuth2TokenValidator<Jwt> audienceValidator = new JwtAudienceValidator(List.of(allowedAudience));
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator(Duration.ofSeconds(60));
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator, withTimestamp);
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    public static class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final List<String> allowedAudiences;
        public JwtAudienceValidator(List<String> allowedAudiences) {
            this.allowedAudiences = allowedAudiences;
        }
        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token has no audience", null));
            }
            boolean hasValidAudience = audiences.stream().anyMatch(allowedAudiences::contains);
            if (!hasValidAudience) {
                return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Token audience is not allowed", null));
            }
            return OAuth2TokenValidatorResult.success();
        }
    }
}
