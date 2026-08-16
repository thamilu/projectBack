package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when domain invariants are violated.
 *
 * <p>Extends {@link BusinessException} to integrate with the existing {@code
 * GlobalExceptionHandler} pipeline. Carries structured field-level context for API error mapping.
 *
 * <h3>1. Field-level validation (preferred for single-field entity constraints):</h3>
 *
 * <pre>{@code
 * throw new DomainValidationException("email", "has invalid format");
 * // message  → "Domain validation failed — [email]: has invalid format"
 * // errorCode → "DOMAIN_VALIDATION_ERROR"
 * // getField() → "email", getViolation() → "has invalid format"
 * }</pre>
 *
 * <h3>2. Explicit message + error code (for invariants not tied to a single field, or
 * where a distinct, stable, client-facing error code is required):</h3>
 *
 * <pre>{@code
 * throw DomainValidationException.withErrorCode("userProfile must not be null", "NULL_USER_PROFILE");
 * // message   → "userProfile must not be null"
 * // errorCode → "NULL_USER_PROFILE"
 * // getField() → null, getViolation() → "userProfile must not be null"
 * }</pre>
 */
public class DomainValidationException extends BusinessException {

    private static final String DEFAULT_ERROR_CODE = "DOMAIN_VALIDATION_ERROR";

    private final String field;
    private final String violation;

    public DomainValidationException(String field, String violation) {
        super(
                buildFieldMessage(
                        requireNonBlank(field, "field"),
                        requireNonBlank(violation, "violation")),
                DEFAULT_ERROR_CODE,
                HttpStatus.BAD_REQUEST);
        this.field = field;
        this.violation = violation;
        addDetail("field", field);
        addDetail("violation", violation);
    }

    private DomainValidationException(String message, String errorCode, HttpStatus status) {
        super(message, errorCode, status);
        this.field = null;
        this.violation = message;
        addDetail("violation", message);
    }

    /**
     * Creates a domain validation exception with an explicit, caller-supplied error code
     * and a fully-formed message. Intended for invariants that are not tied to a single
     * field (e.g., aggregate-level or null-argument checks) and for callers that require
     * a distinct, stable error code for client-side/API error handling.
     *
     * @param message   a complete, human-readable description of the violation
     * @param errorCode a stable, machine-readable error code (e.g., {@code "NULL_USER_PROFILE"})
     */
    public static DomainValidationException withErrorCode(String message, String errorCode) {
        return new DomainValidationException(
                requireNonBlank(message, "message"),
                requireNonBlank(errorCode, "errorCode"),
                HttpStatus.BAD_REQUEST);
    }

    private static String buildFieldMessage(String field, String violation) {
        return String.format("Domain validation failed — [%s]: %s", field, violation);
    }

    private static String requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
        return value;
    }

    /**
     * @return the field name when this exception was created via
     *         {@link #DomainValidationException(String, String)}, or {@code null} when
     *         created via {@link #withErrorCode(String, String)}.
     */
    public String getField() {
        return field;
    }

    public String getViolation() {
        return violation;
    }
}
