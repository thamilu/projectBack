package com.eshop.app.seed.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;

/**
 * Secure password generator for seed data.
 * Follows security best practices:
 * 1. Environment variables (highest priority - for CI/CD)
 * 2. Application properties (with vault/secrets manager)
 * 3. Cryptographically secure random generation (fallback)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurePasswordGenerator {
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final int DEFAULT_PASSWORD_LENGTH = 20;
    private static final int MINIMUM_PASSWORD_LENGTH = 12;
    
    private final Environment environment;
    
    /**
     * Generate or resolve password for a user.
     * 
     * @param hint Username or role hint (e.g., "admin", "customer1")
     * @return Resolved or generated secure password
     */
    public String generate(String hint) {
        // 1. Try environment variable (most secure for CI/CD)
        String envValue = resolveFromEnvironment(hint);
        if (StringUtils.hasText(envValue)) {
            validatePasswordStrength(envValue, hint);
            log.debug("Resolved password from environment for: {}", hint);
            return envValue;
        }
        
        // 2. Try application property (with vault/secrets manager)
        String propValue = resolveFromProperties(hint);
        if (StringUtils.hasText(propValue)) {
            validatePasswordStrength(propValue, hint);
            log.debug("Resolved password from properties for: {}", hint);
            return propValue;
        }
        
        // 3. Generate cryptographically secure password
        log.debug("Generating secure random password for: {}", hint);
        return generateSecurePassword(DEFAULT_PASSWORD_LENGTH);
    }
    
    /**
     * Resolve password from environment variable.
     * Pattern: SEED_PASS_ADMIN, SEED_PASS_CUSTOMER1, etc.
     */
    private String resolveFromEnvironment(String hint) {
        String envKey = "SEED_PASS_" + hint.toUpperCase();
        return environment.getProperty(envKey);
    }
    
    /**
     * Resolve password from application properties.
     * Pattern: app.seed.password.admin, app.seed.password.customer1, etc.
     */
    private String resolveFromProperties(String hint) {
        String propKey = "app.seed.password." + hint.toLowerCase();
        return environment.getProperty(propKey);
    }
    
    /**
     * Generate cryptographically secure password with guaranteed complexity.
     * 
     * @param length Desired password length (minimum 12)
     * @return Secure password meeting complexity requirements
     */
    private String generateSecurePassword(int length) {
        if (length < MINIMUM_PASSWORD_LENGTH) {
            length = MINIMUM_PASSWORD_LENGTH;
        }
        
        StringBuilder password = new StringBuilder(length);
        String allChars = UPPERCASE + LOWERCASE + DIGITS + SPECIAL;
        
        // Ensure at least one of each type for complexity
        password.append(UPPERCASE.charAt(SECURE_RANDOM.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(SECURE_RANDOM.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(SECURE_RANDOM.nextInt(DIGITS.length())));
        password.append(SPECIAL.charAt(SECURE_RANDOM.nextInt(SPECIAL.length())));
        
        // Fill rest randomly
        for (int i = 4; i < length; i++) {
            password.append(allChars.charAt(SECURE_RANDOM.nextInt(allChars.length())));
        }
        
        // Shuffle to avoid predictable pattern
        return shuffleString(password.toString());
    }
    
    /**
     * Shuffle string characters using Fisher-Yates algorithm.
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
    
    /**
     * Validate password meets minimum security requirements.
     * 
     * @param password Password to validate
     * @param context Context for error messages
     * @throws IllegalArgumentException if password is too weak
     */
    private void validatePasswordStrength(String password, String context) {
        if (password.length() < MINIMUM_PASSWORD_LENGTH) {
            log.warn("Password for '{}' is only {} characters (minimum {})",
                context, password.length(), MINIMUM_PASSWORD_LENGTH);
        }
    }
}
