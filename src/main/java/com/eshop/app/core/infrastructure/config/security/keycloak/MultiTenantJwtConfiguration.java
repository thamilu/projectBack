package com.eshop.app.core.infrastructure.config.security.keycloak;

import com.eshop.app.core.infrastructure.config.properties.SecurityProperties;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.time.Duration;
import java.util.*;

/**
 * Hardened multi-tenant JWT decoder: routes an incoming token to the correct
 * issuer-specific {@link JwtDecoder} (user realm vs. admin realm), each performing full
 * JWKS-based signature verification plus issuer/audience/timestamp validation.
 *
 * <p>This is the sole {@link JwtDecoder} bean in the application ({@code @Primary}),
 * active in every profile except {@code test}.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class MultiTenantJwtConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String userRealmIssuer;

    private final SecurityProperties securityProperties;

    /**
     * Fails application startup immediately, with a clear message, if either issuer URI
     * is blank — rather than silently proceeding with a guessed/placeholder value that
     * would only surface as a mysterious runtime authentication failure later.
     */
    @PostConstruct
    void validateConfiguration() {
        Assert.hasText(userRealmIssuer,
                "spring.security.oauth2.resourceserver.jwt.issuer-uri must be configured");
        Assert.hasText(securityProperties.getAdmin().getIssuerUri(),
                "app.security.admin.issuer-uri must be configured");
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        log.info("Configuring hardened multi-tenant JWT decoder [allowed audiences: {}]",
                securityProperties.getJwt().getAllowedAudiences());

        String adminRealmIssuer = securityProperties.getAdmin().getIssuerUri();
        Map<String, JwtDecoder> decoders = Map.of(
                userRealmIssuer, createDecoder(userRealmIssuer, true),
                adminRealmIssuer, createDecoder(adminRealmIssuer, false));

        return new IssuerRoutingJwtDecoder(decoders);
    }

    /**
     * Builds a hardened, signature-verifying decoder for one issuer.
     *
     * @param enforceEmailVerifiedEligible whether this issuer's decoder should apply
     *        {@link EmailVerifiedValidator} when {@code security.jwt.enforce-email-verified}
     *        is enabled. {@code false} for the admin realm: administrators may be
     *        provisioned outside the self-registration/email-verification flow, so
     *        bundling that check into the same flag as the user realm would silently
     *        lock out legitimate admins the moment the flag is turned on.
     */
    private JwtDecoder createDecoder(String issuer, boolean enforceEmailVerifiedEligible) {
        // Explicit connect/read timeouts: NimbusJwtDecoder.withIssuerLocation(...).build()
        // performs a synchronous, blocking HTTP call to the issuer's OIDC discovery
        // endpoint right here, during application startup (inside this @Bean method).
        // Spring's default RestTemplate has no bounded timeout, so an unreachable
        // Keycloak at deploy time would otherwise hang application startup indefinitely
        // instead of failing within a few seconds with a clear cause.
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(10).toMillis());

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withIssuerLocation(issuer)
                .restOperations(new RestTemplate(requestFactory))
                // Restricts accepted signature algorithms to asymmetric RS* — defense in
                // depth against algorithm-confusion attacks (e.g. an attacker attempting
                // to have an RS256-configured verifier treat a token as HS256-signed
                // using the RSA public key as an HMAC secret).
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        // Explicit issuer validator only — NOT JwtValidators.createDefaultWithIssuer(issuer),
        // which bundles its own JwtTimestampValidator with a fixed ~60s default skew. That
        // extra validator ran ALONGSIDE the explicitly-configured one below with no
        // coordination: since a DelegatingOAuth2TokenValidator requires every validator to
        // pass, the more restrictive of the two silently won. With clockSkewSeconds
        // configured to 300s (5 minutes) but the hidden default capping it at ~60s, the
        // administrator's actual intended tolerance was never really in effect. There is
        // now exactly one timestamp validator, using exactly the configured value.
        validators.add(new JwtIssuerValidator(issuer));
        validators.add(new JwtTimestampValidator(Duration.ofSeconds(securityProperties.getJwt().getClockSkewSeconds())));
        validators.add(new JwtAudienceValidator(
                securityProperties.getJwt().getAllowedAudiences(),
                securityProperties.getJwt().isRequireAudience()));

        if (enforceEmailVerifiedEligible && securityProperties.getJwt().isEnforceEmailVerified()) {
            log.info("Security Hardening: Enforcing email_verified claim for issuer {}", issuer);
            validators.add(new EmailVerifiedValidator());
        }

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    /**
     * Routes an incoming token to the correct tenant decoder by issuer, using EXACT
     * string match only (no prefix/substring matching). Package-private and free of any
     * dependency on the outer configuration class, so it can be unit tested directly
     * without a Spring context.
     *
     * <p>The issuer used for routing is read from the token's payload BEFORE signature
     * verification (structurally necessary for multi-issuer routing — see
     * {@code JwtIssuerAuthenticationManagerResolver} in Spring Security itself for the
     * same pattern). This is safe because each decoder's own {@link JwtIssuerValidator}
     * strictly re-checks the token's issuer claim against the exact issuer that
     * decoder's JWKS was fetched for — a token whose claimed issuer doesn't exactly
     * match a configured decoder is rejected before this method is even reached, and a
     * forged/mismatched token that somehow reached a decoder would still fail signature
     * verification against that decoder's fixed JWKS key material.
     */
    static final class IssuerRoutingJwtDecoder implements JwtDecoder {

        private final Map<String, JwtDecoder> decoders;

        IssuerRoutingJwtDecoder(Map<String, JwtDecoder> decoders) {
            this.decoders = Map.copyOf(decoders);
        }

        @Override
        public Jwt decode(String token) throws JwtException {
            String issuer = extractUnverifiedIssuer(token);
            JwtDecoder decoder = decoders.get(issuer);
            if (decoder == null) {
                throw new BadJwtException("Unknown issuer: " + issuer);
            }
            return decoder.decode(token);
        }

        private String extractUnverifiedIssuer(String token) {
            try {
                JWT jwt = JWTParser.parse(token);
                String issuer = jwt.getJWTClaimsSet().getIssuer();
                if (issuer == null) {
                    throw new BadJwtException("Missing issuer claim");
                }
                return issuer;
            } catch (ParseException e) {
                // Do not chain the raw ParseException as the cause: its message can
                // include fragments of the malformed token content, which should not be
                // forwarded toward any downstream error-response path.
                throw new BadJwtException("Failed to parse token");
            }
        }
    }

    /**
     * Validates that the JWT audience matches one of the values this backend accepts.
     */
    public static class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final List<String> allowedAudiences;
        private final boolean requireAudience;

        public JwtAudienceValidator(List<String> allowedAudiences, boolean requireAudience) {
            this.allowedAudiences = allowedAudiences;
            this.requireAudience = requireAudience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty()) {
                if (requireAudience) {
                    log.warn("JWT Rejected: Missing required audience claim. Subject: {}", jwt.getSubject());
                    return OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_token", "Token must contain an audience claim", null));
                }
                // Lenient mode (default, preserves prior behavior): Keycloak omits 'aud'
                // entirely unless an audience mapper is explicitly configured on the
                // client, so requiring it unconditionally would break every deployment
                // that hasn't set that mapper up. Set security.jwt.require-audience=true
                // once every client in use is confirmed to populate this claim.
                log.debug("JWT Verification: Token has no audience claim. Skipping audience check.");
                return OAuth2TokenValidatorResult.success();
            }

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
     * Validates that the JWT subject has verified their email address. Only applied to
     * the user-realm decoder — see {@link #createDecoder}'s
     * {@code enforceEmailVerifiedEligible} parameter.
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
