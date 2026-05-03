package com.eshop.app.config.startup;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;

/**
 * CRITICAL-004 FIX: Startup Credential Validation
 * 
 * <p>Validates that critical secrets and credentials are properly configured
 * at application startup. Fails fast if required credentials are missing
 * or use unsafe defaults.
 * 
 * <h2>Validated Configuration:</h2>
 * <ul>
 *   <li>Database credentials (username, password, URL)</li>
 *   <li>JWT secrets (must be strong and not default values)</li>
 *   <li>Payment gateway credentials (when enabled)</li>
 *   <li>OAuth2/Keycloak credentials (when enabled)</li>
 * </ul>
 * 
 * <h2>Security Benefits:</h2>
 * <ul>
 *   <li>Prevents application start with missing secrets</li>
 *   <li>Detects unsafe default values</li>
 *   <li>Forces proper environment configuration</li>
 *   <li>Reduces production security incidents</li>
 * </ul>
 * 
 * @author EShop Security Team
 * @version 1.0
 * @since 2025-12-20
 */
@Configuration
@Profile("!test")
@Slf4j
public class CredentialValidator {

    @Value("${app.startup.validate-credentials:true}")
    private boolean enforceValidation;
    
    // Unsafe default values that should never be used in production
    private static final String[] UNSAFE_DEFAULTS = {
        "changeme", "password", "secret", "default", "admin", 
        "test", "demo", "example", "your-secret-here"
    };
    
    private static final int MIN_JWT_SECRET_LENGTH = 32;
    
    // Database
    @Value("${spring.datasource.url:}")
    private String databaseUrl;
    
    @Value("${spring.datasource.username:}")
    private String databaseUsername;
    
    @Value("${spring.datasource.password:}")
    private String databasePassword;
    
    // JWT
    @Value("${jwt.secret:}")
    private String jwtSecret;
    
    // Stripe (optional)
    @Value("${stripe.enabled:false}")
    private boolean stripeEnabled;
    
    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;
    
    @Value("${stripe.public-key:}")
    private String stripePublicKey;
    
    // Razorpay (optional)
    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;
    
    @Value("${razorpay.key-id:}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key-secret:}")
    private String razorpayKeySecret;
    
    // Keycloak (optional)
    @Value("${keycloak.enabled:false}")
    private boolean keycloakEnabled;
    
    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
    private String keycloakIssuerUri;
    
    @Value("${spring.profiles.active:}")
    private String activeProfile;
    
    /**
     * Validates credentials at application startup.
     * Throws IllegalStateException if critical configuration is missing or unsafe.
     */
    @PostConstruct
    public void validateCredentials() {
        log.info("🔐 Starting credential validation for profile: {} (enforceValidation={})", activeProfile, enforceValidation);

        if (!enforceValidation || (activeProfile != null && activeProfile.contains("test"))) {
            log.info("Skipping credential validation (enforceValidation={} and activeProfile={})", enforceValidation, activeProfile);
            return;
        }
        
        boolean isProd = activeProfile.contains("prod");
        
        try {
            // Always validate database credentials
            validateDatabaseCredentials(isProd);
            
            // Always validate JWT secret
            validateJwtSecret(isProd);
            
            // Conditional validation based on feature flags
            if (stripeEnabled) {
                validateStripeCredentials(isProd);
            }
            
            if (razorpayEnabled) {
                validateRazorpayCredentials(isProd);
            }
            
            if (keycloakEnabled) {
                validateKeycloakCredentials(isProd);
            }
            
            log.info("✅ All credential validations passed");
            
        } catch (IllegalStateException e) {
            log.error("❌ CREDENTIAL VALIDATION FAILED: {}", e.getMessage());
            throw e;
        }
    }
    
    private void validateDatabaseCredentials(boolean isProd) {
        if (!StringUtils.hasText(databaseUrl)) {
            throw new IllegalStateException(
                "Database URL is not configured. Set DATABASE_URL environment variable."
            );
        }
        
        if (!StringUtils.hasText(databaseUsername)) {
            throw new IllegalStateException(
                "Database username is not configured. Set DATABASE_USERNAME environment variable."
            );
        }
        
        if (!StringUtils.hasText(databasePassword)) {
            throw new IllegalStateException(
                "Database password is not configured. Set DATABASE_PASSWORD environment variable."
            );
        }
        
        if (isProd && isUnsafeValue(databasePassword)) {
            throw new IllegalStateException(
                "Database password contains unsafe default value. Use a strong password."
            );
        }
        
        log.debug("✓ Database credentials validated");
    }
    
    private void validateJwtSecret(boolean isProd) {
        if (!StringUtils.hasText(jwtSecret)) {
            throw new IllegalStateException(
                "JWT secret is not configured. Set JWT_SECRET environment variable."
            );
        }
        
        if (jwtSecret.length() < MIN_JWT_SECRET_LENGTH) {
            throw new IllegalStateException(String.format(
                "JWT secret is too short (%d characters). Minimum required: %d characters.",
                jwtSecret.length(), MIN_JWT_SECRET_LENGTH
            ));
        }
        
        if (isUnsafeValue(jwtSecret)) {
            throw new IllegalStateException(
                "JWT secret contains unsafe default value. Use a strong, randomly generated secret."
            );
        }
        
        log.debug("✓ JWT secret validated");
    }
    
    private void validateStripeCredentials(boolean isProd) {
        if (!StringUtils.hasText(stripeSecretKey)) {
            throw new IllegalStateException(
                "Stripe is enabled but secret key is not configured. Set STRIPE_SECRET_KEY environment variable."
            );
        }
        
        if (!StringUtils.hasText(stripePublicKey)) {
            throw new IllegalStateException(
                "Stripe is enabled but public key is not configured. Set STRIPE_PUBLIC_KEY environment variable."
            );
        }
        
        if (isProd && isUnsafeValue(stripeSecretKey)) {
            throw new IllegalStateException(
                "Stripe secret key contains unsafe default value."
            );
        }
        
        log.debug("✓ Stripe credentials validated");
    }
    
    private void validateRazorpayCredentials(boolean isProd) {
        if (!StringUtils.hasText(razorpayKeyId)) {
            throw new IllegalStateException(
                "Razorpay is enabled but key ID is not configured. Set RAZORPAY_KEY_ID environment variable."
            );
        }
        
        if (!StringUtils.hasText(razorpayKeySecret)) {
            throw new IllegalStateException(
                "Razorpay is enabled but key secret is not configured. Set RAZORPAY_KEY_SECRET environment variable."
            );
        }
        
        if (isProd && isUnsafeValue(razorpayKeySecret)) {
            throw new IllegalStateException(
                "Razorpay key secret contains unsafe default value."
            );
        }
        
        log.debug("✓ Razorpay credentials validated");
    }
    
    private void validateKeycloakCredentials(boolean isProd) {
        if (!StringUtils.hasText(keycloakIssuerUri)) {
            throw new IllegalStateException(
                "Keycloak is enabled but issuer URI is not configured. " +
                "Set KEYCLOAK_AUTH_SERVER_URL and KEYCLOAK_REALM environment variables."
            );
        }
        
        if (isProd && keycloakIssuerUri.contains("localhost")) {
            log.warn("⚠️  Keycloak issuer URI points to localhost in production environment");
        }
        
        log.debug("✓ Keycloak configuration validated");
    }
    
    /**
     * Checks if a value contains known unsafe defaults
     */
    private boolean isUnsafeValue(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        
        String lowerValue = value.toLowerCase();
        for (String unsafeDefault : UNSAFE_DEFAULTS) {
            if (lowerValue.contains(unsafeDefault)) {
                return true;
            }
        }
        
        return false;
    }
}
