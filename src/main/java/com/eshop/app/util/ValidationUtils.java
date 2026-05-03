package com.eshop.app.util;

import org.springframework.util.StringUtils;
import java.util.Collection;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations across the application
 * 
 * Centralizes validation logic following DRY principle to avoid duplicate validation code
 * Provides reusable methods for null checks, string validation, numeric validation, etc.
 * 
 * @author E-Shop Team
 * @version 1.0
 */
public final class ValidationUtils {
    
    private ValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    // ==================== NULL & EMPTY CHECKS ====================
    
    /**
     * Check if value is null or empty string
     * 
     * @param value String to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String value) {
        return value == null || value.isEmpty();
    }
    
    /**
     * Check if value is null or contains only whitespace
     * 
     * @param value String to check
     * @return true if null or blank, false otherwise
     */
    public static boolean isNullOrBlank(String value) {
        return !StringUtils.hasText(value);
    }
    
    /**
     * Check if value is not null and not empty
     * 
     * @param value String to check
     * @return true if has text, false otherwise
     */
    public static boolean hasValue(String value) {
        return StringUtils.hasText(value);
    }
    
    /**
     * Check if collection is null or empty
     * 
     * @param collection Collection to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    
    /**
     * Check if array is null or empty
     * 
     * @param array Array to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
    
    /**
     * Check if object is null
     * 
     * @param obj Object to check
     * @return true if null, false otherwise
     */
    public static boolean isNull(Object obj) {
        return obj == null;
    }
    
    /**
     * Check if object is not null
     * 
     * @param obj Object to check
     * @return true if not null, false otherwise
     */
    public static boolean isNotNull(Object obj) {
        return obj != null;
    }
    
    // ==================== NUMERIC VALIDATION ====================
    
    /**
     * Validate that a number is positive (greater than 0)
     * 
     * @param value Number to validate
     * @return true if positive, false otherwise
     */
    public static boolean isPositive(long value) {
        return value > 0;
    }
    
    /**
     * Validate that a number is not negative (greater than or equal to 0)
     * 
     * @param value Number to validate
     * @return true if not negative, false otherwise
     */
    public static boolean isNotNegative(long value) {
        return value >= 0;
    }
    
    /**
     * Validate that a double is positive
     * 
     * @param value Number to validate
     * @return true if positive, false otherwise
     */
    public static boolean isPositive(double value) {
        return value > 0.0;
    }
    
    /**
     * Validate that a number is within a range (inclusive)
     * 
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return true if value is within range, false otherwise
     */
    public static boolean isBetween(long value, long min, long max) {
        return value >= min && value <= max;
    }
    
    // ==================== STRING VALIDATION ====================
    
    /**
     * Email pattern for basic validation
     */
    private static final Pattern EMAIL_PATTERN = 
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$");
    
    /**
     * Validate email format
     * Note: This is a basic check. For strict validation, use email verification service
     * 
     * @param email Email to validate
     * @return true if valid email format, false otherwise
     */
    public static boolean isValidEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Phone pattern for basic validation (supports various formats)
     */
    private static final Pattern PHONE_PATTERN = 
            Pattern.compile("^[+]?[0-9]{10,15}$");
    
    /**
     * Validate phone number format
     * 
     * @param phone Phone number to validate
     * @return true if valid phone format, false otherwise
     */
    public static boolean isValidPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone.replaceAll("[\\s\\-()]", "")).matches();
    }
    
    /**
     * Validate string length is within bounds
     * 
     * @param value String to validate
     * @param minLength Minimum length (inclusive)
     * @param maxLength Maximum length (inclusive)
     * @return true if length is valid, false otherwise
     */
    public static boolean isLengthValid(String value, int minLength, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return minLength == 0;
        }
        int length = value.length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * Check if string contains only alphanumeric characters
     * 
     * @param value String to check
     * @return true if alphanumeric, false otherwise
     */
    public static boolean isAlphanumeric(String value) {
        return StringUtils.hasText(value) && value.matches("^[a-zA-Z0-9]+$");
    }
    
    /**
     * Sanitize string by trimming and removing null
     * 
     * @param value String to sanitize
     * @return Trimmed string or null if input is null/empty
     */
    public static String sanitize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
    
    // ==================== CONDITIONAL VALIDATION ====================
    
    /**
     * Require non-null value, throw exception if null
     * 
     * @param value Value to check
     * @param message Error message
     * @param <T> Type of value
     * @return The non-null value
     * @throws IllegalArgumentException if value is null
     */
    public static <T> T requireNonNull(T value, String message) {
        return Objects.requireNonNull(value, message);
    }
    
    /**
     * Require non-empty string, throw exception if empty
     * 
     * @param value String to check
     * @param message Error message
     * @return The non-empty string
     * @throws IllegalArgumentException if string is null or empty
     */
    public static String requireNonEmpty(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
    
    /**
     * Require non-empty collection, throw exception if empty
     * 
     * @param collection Collection to check
     * @param message Error message
     * @param <T> Element type
     * @return The non-empty collection
     * @throws IllegalArgumentException if collection is null or empty
     */
    public static <T extends Collection<?>> T requireNonEmpty(T collection, String message) {
        if (isNullOrEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
        return collection;
    }
    
    /**
     * Require positive number, throw exception if not positive
     * 
     * @param value Number to check
     * @param message Error message
     * @return The positive number
     * @throws IllegalArgumentException if number is not positive
     */
    public static long requirePositive(long value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
    
    /**
     * Require value within range, throw exception if out of range
     * 
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @param message Error message
     * @return The value if within range
     * @throws IllegalArgumentException if value is out of range
     */
    public static long requireBetween(long value, long min, long max, String message) {
        if (!isBetween(value, min, max)) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
