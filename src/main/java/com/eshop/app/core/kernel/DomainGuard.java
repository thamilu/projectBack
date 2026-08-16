package com.eshop.app.core.kernel;

import com.eshop.app.core.exception.business.DomainValidationException;
import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Centralized domain validation utility.
 *
 * <p>Eliminates scattered validation logic across entities by providing reusable, field-aware
 * validation methods that throw structured {@link DomainValidationException} on failure.
 *
 * <p>All methods return the validated (and trimmed, where applicable) value for fluent inline use:
 *
 * <pre>{@code
 * this.email = DomainGuard.requireValidEmail(email);
 * }</pre>
 */
public final class DomainGuard {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{6,14}$");

    private static final int MAX_TOTP_SECRET_LENGTH = 64;

    private static final Pattern URL_PATTERN =
            Pattern.compile(
                    "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");

    private static final Pattern LANGUAGE_PATTERN =
            Pattern.compile("^[a-zA-Z]{2,8}(?:-[a-zA-Z0-9]{2,8})*$");

    private DomainGuard() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ensures value is not null or blank.
     *
     * @param value value to validate
     * @param field field name for error context
     * @return validated value (trimmed)
     * @throws DomainValidationException if null or blank
     */
    public static String requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DomainValidationException(field, "must not be blank");
        }
        return value.trim();
    }

    /**
     * Ensures object is not null.
     *
     * @param value value to validate
     * @param field field name for error context
     * @param <T> value type
     * @return validated value
     * @throws DomainValidationException if null
     */
    public static <T> T requireNotNull(T value, String field) {
        if (value == null) {
            throw new DomainValidationException(field, "must not be null");
        }
        return value;
    }

    /**
     * Validates email format using a standard pattern.
     *
     * @param email email address to validate
     * @return validated and trimmed email
     * @throws DomainValidationException if null, blank, or invalid format
     */
    public static String requireValidEmail(String email) {
        String validated = requireNotBlank(email, "email");
        if (!EMAIL_PATTERN.matcher(validated).matches()) {
            throw new DomainValidationException("email", "has invalid format: " + email);
        }
        return validated;
    }

    /**
     * Validates phone format. Standardizes to strict E.164 format. Supports optional country code,
     * leading +, and strips spaces, dashes, parentheses.
     *
     * @param phone phone number to validate
     * @return normalized E.164 phone string
     * @throws DomainValidationException if null, blank, or invalid format
     */
    public static String requireValidPhone(String phone) {
        String validated = requireNotBlank(phone, "phone");
        // Normalize the phone number by stripping formatting characters
        String normalized = validated.replaceAll("[\\s\\-\\(\\)]", "");
        if (!PHONE_PATTERN.matcher(normalized).matches()) {
            throw new DomainValidationException("phone", "has invalid format: " + phone);
        }
        return normalized;
    }

    /**
     * Validates string does not exceed maximum length.
     *
     * @param value value to validate
     * @param field field name for error context
     * @param maxLength maximum allowed length
     * @return validated value
     * @throws DomainValidationException if null, blank, or exceeds length
     */
    public static String requireMaxLength(String value, String field, int maxLength) {
        requireNotBlank(value, field);
        if (value.length() > maxLength) {
            throw new DomainValidationException(
                    field,
                    String.format(
                            "must not exceed %d characters, got %d", maxLength, value.length()));
        }
        return value;
    }

    /**
     * Validates TOTP secret constraints (not blank, max 64 characters).
     *
     * @param secret TOTP shared secret to validate
     * @return validated secret
     * @throws DomainValidationException if null, blank, or exceeds length
     */
    public static String requireValidTotpSecret(String secret) {
        String validated = requireNotBlank(secret, "twoFactorSecret");
        return requireMaxLength(validated, "twoFactorSecret", MAX_TOTP_SECRET_LENGTH);
    }

    /**
     * Validates name constraints (not null, not blank, and maximum length) without trimming the
     * value.
     *
     * @param name name to validate
     * @param field field name for error context
     * @param maxLength maximum allowed length
     * @return validated untrimmed name
     * @throws DomainValidationException if invalid
     */
    public static String requireValidName(String name, String field, int maxLength) {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException(field, "must not be blank");
        }
        if (name.length() > maxLength) {
            throw new DomainValidationException(
                    field,
                    String.format(
                            "must not exceed %d characters, got %d", maxLength, name.length()));
        }
        return name;
    }

    /**
     * Ensures the given date is in the past or present.
     *
     * @param date date to validate
     * @param field field name for error context
     * @return validated date
     * @throws DomainValidationException if in the future
     */
    public static LocalDate requirePastOrPresent(LocalDate date, String field) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new DomainValidationException(field, field + " cannot be in the future");
        }
        return date;
    }

    /**
     * Validates a URL pattern.
     *
     * @param url URL to validate
     * @param field field name for error context
     * @return validated URL
     * @throws DomainValidationException if format is invalid
     */
    public static String requireValidUrl(String url, String field) {
        String validated = requireNotBlank(url, field);
        if (!URL_PATTERN.matcher(validated).matches()) {
            throw new DomainValidationException(field, "has invalid URL format: " + url);
        }
        return validated;
    }

    /**
     * Validates BCP 47 language code pattern.
     *
     * @param code language code to validate
     * @param field field name for error context
     * @return validated language code
     * @throws DomainValidationException if format is invalid
     */
    public static String requireValidLanguageCode(String code, String field) {
        String validated = requireNotBlank(code, field);
        if (!LANGUAGE_PATTERN.matcher(validated).matches()) {
            throw new DomainValidationException(field, "has invalid language code format: " + code);
        }
        return validated;
    }

    /**
     * Validates latitude is within the valid range [-90, 90]. Null-safe — returns null if input is
     * null.
     *
     * @param latitude latitude value to validate
     * @param field field name for error context
     * @return validated latitude (or null)
     * @throws DomainValidationException if out of range
     */
    public static Double requireValidLatitude(Double latitude, String field) {
        if (latitude != null && (latitude < -90.0 || latitude > 90.0)) {
            throw new DomainValidationException(
                    field, "Latitude must be between -90 and 90, got " + latitude);
        }
        return latitude;
    }

    /**
     * Validates longitude is within the valid range [-180, 180]. Null-safe — returns null if input
     * is null.
     *
     * @param longitude longitude value to validate
     * @param field field name for error context
     * @return validated longitude (or null)
     * @throws DomainValidationException if out of range
     */
    public static Double requireValidLongitude(Double longitude, String field) {
        if (longitude != null && (longitude < -180.0 || longitude > 180.0)) {
            throw new DomainValidationException(
                    field, "Longitude must be between -180 and 180, got " + longitude);
        }
        return longitude;
    }
}
