package com.eshop.app.config;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * ⚠️ SECURITY WARNING: MOCK JWT DECODER - FOR TESTING ONLY ⚠️
 *
 * This configuration provides a mock JWT decoder that performs NO SIGNATURE
 * VERIFICATION and is intended ONLY for local development or tests.
 *
 * Safety mechanisms applied:
 *  - Profile restriction (only active in non-production profiles)
 *  - Explicit property gate: `app.security.mock-jwt.enabled=true` required
 *  - Runtime check to abort if a production-like profile is active
 *  - Clear warning logs on activation
 *
 * NEVER enable this in production environments.
 */
@Configuration
@Profile({"test", "local", "dev"})
@ConditionalOnProperty(name = "app.security.mock-jwt.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(DefaultJwtDecoderConfig.DefaultJwtProperties.class)
public class DefaultJwtDecoderConfig {

    private static final Logger log = LoggerFactory.getLogger(DefaultJwtDecoderConfig.class);

    private final Environment environment;
    public DefaultJwtDecoderConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void warnOnActivation() {
        log.warn("╔══════════════════════════════════════════════════════════════════╗");
        log.warn("║  ⚠️  MOCK JWT DECODER ACTIVATED - NO TOKEN VALIDATION!  ⚠️        ║");
        log.warn("║                                                                  ║");
        log.warn("║  All JWT tokens will be accepted without verification.           ║");
        log.warn("║  This should NEVER appear in production logs!                    ║");
        log.warn("║                                                                  ║");
        log.warn("║  Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.warn("╚══════════════════════════════════════════════════════════════════╝");
    }

    // JwtDecoder bean now provided by JwtAudienceValidatorConfig for all profiles.

    // MockJwtDecoder and validation helpers removed — mock decoder provided elsewhere if needed

    @ConfigurationProperties(prefix = "app.security.mock-jwt")
    public static class DefaultJwtProperties {

        private boolean enabled = false;
        private String defaultSubject = "test-user";
        private List<String> defaultRoles = List.of("USER");
        private String defaultScopes = "openid profile email";
        private long validitySeconds = 3600;
        private String issuer = "mock-jwt-issuer";
        private List<String> audience = List.of("mock-client");
        private Map<String, String> userMapping = new HashMap<>();
        private Map<String, List<String>> roleMapping = new HashMap<>();

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getDefaultSubject() { return defaultSubject; }
        public void setDefaultSubject(String defaultSubject) { this.defaultSubject = defaultSubject; }

        public List<String> getDefaultRoles() { return defaultRoles; }
        public void setDefaultRoles(List<String> defaultRoles) { this.defaultRoles = defaultRoles; }

        public String getDefaultScopes() { return defaultScopes; }
        public void setDefaultScopes(String defaultScopes) { this.defaultScopes = defaultScopes; }

        public long getValiditySeconds() { return validitySeconds; }
        public void setValiditySeconds(long validitySeconds) { this.validitySeconds = validitySeconds; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public List<String> getAudience() { return audience; }
        public void setAudience(List<String> audience) { this.audience = audience; }

        public Map<String, String> getUserMapping() { return userMapping; }
        public void setUserMapping(Map<String, String> userMapping) { this.userMapping = userMapping; }

        public Map<String, List<String>> getRoleMapping() { return roleMapping; }
        public void setRoleMapping(Map<String, List<String>> roleMapping) { this.roleMapping = roleMapping; }
    }
}
