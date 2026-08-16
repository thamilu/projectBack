package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when an attempt is made to create or update a resource using a value that
 * must be unique but already exists (e.g., duplicate email, username, or SKU).
 *
 * <p>Extends {@link BusinessException} and always maps to HTTP {@code 409 Conflict},
 * regardless of whether a root cause is supplied.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * throw new DuplicateResourceException("User", "email", "john@example.com");
 * // → "User with email 'john@example.com' already exists"
 * }</pre>
 */
public class DuplicateResourceException extends BusinessException {

    private static final String ERROR_CODE = "DUPLICATE_RESOURCE";

    private final String resourceName;
    private final String fieldName;
    private final String fieldValue;

    public DuplicateResourceException(String resourceName, String fieldName, String fieldValue) {
        super(
                buildMessage(
                        requireNonBlank(resourceName, "resourceName"),
                        requireNonBlank(fieldName, "fieldName"),
                        requireNonBlank(fieldValue, "fieldValue")),
                ERROR_CODE,
                HttpStatus.CONFLICT);
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        addDetail("resourceName", resourceName);
        addDetail("fieldName", fieldName);
        addDetail("fieldValue", fieldValue);
    }

    public DuplicateResourceException(String message) {
        super(requireNonBlank(message, "message"), ERROR_CODE, HttpStatus.CONFLICT);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public DuplicateResourceException(String message, Throwable cause) {
        super(requireNonBlank(message, "message"), ERROR_CODE, HttpStatus.CONFLICT, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    private static String buildMessage(String resourceName, String fieldName, String fieldValue) {
        return String.format("%s with %s '%s' already exists", resourceName, fieldName, fieldValue);
    }

    private static String requireNonBlank(String value, String paramName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(paramName + " must not be blank");
        }
        return value;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
