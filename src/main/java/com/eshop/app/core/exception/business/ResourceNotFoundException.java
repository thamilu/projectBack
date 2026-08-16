package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;
import java.util.Objects;

/**
 * Thrown when a requested resource cannot be found (e.g., lookup by ID, unique
 * identifier, or business key returns no result).
 *
 * <p>Extends {@link BusinessException} and always maps to HTTP {@code 404 Not Found},
 * regardless of whether a root cause is supplied.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * throw new ResourceNotFoundException("User", "id", userId);
 * // → "User not found with id: '42'"
 *
 * // preserving the original lookup failure:
 * try {
 *     return repository.findById(id).orElseThrow();
 * } catch (NoSuchElementException ex) {
 *     throw new ResourceNotFoundException("User not found with id: " + id, ex);
 * }
 * }</pre>
 */
public class ResourceNotFoundException extends BusinessException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;

    public ResourceNotFoundException(String message) {
        super(requireNonBlank(message, "message"), ERROR_CODE, HttpStatus.NOT_FOUND);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(requireNonBlank(message, "message"), ERROR_CODE, HttpStatus.NOT_FOUND, cause);
        this.resourceName = null;
        this.fieldName = null;
        this.fieldValue = null;
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
                buildMessage(
                        requireNonBlank(resourceName, "resourceName"),
                        requireNonBlank(fieldName, "fieldName"),
                        Objects.requireNonNull(fieldValue, "fieldValue must not be null")),
                ERROR_CODE,
                HttpStatus.NOT_FOUND);
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
        addDetail("resourceName", resourceName);
        addDetail("fieldName", fieldName);
        addDetail("fieldValue", fieldValue);
    }

    private static String buildMessage(String resourceName, String fieldName, Object fieldValue) {
        return String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue);
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

    public Object getFieldValue() {
        return fieldValue;
    }
}
