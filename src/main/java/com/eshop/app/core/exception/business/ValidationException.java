package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Business exception representing one or more field-level validation failures.
 * <p>
 * Use {@link #addFieldError(String, String)} or
 * {@link #addFieldError(String, String, Object)} to attach field-specific
 * validation errors before throwing this exception.
 */
@Getter
public class ValidationException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "VALIDATION_ERROR";

    private final List<FieldError> fieldErrors = new ArrayList<>();

    public ValidationException(String message) {
        this(message, DEFAULT_ERROR_CODE, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, String errorCode) {
        this(message, errorCode, HttpStatus.BAD_REQUEST);
    }

    public ValidationException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode, httpStatus);
    }

    /**
     * Returns an unmodifiable view of the collected field errors.
     * Use {@link #addFieldError} to attach additional errors; the returned
     * list cannot be mutated directly.
     */
    public List<FieldError> getFieldErrors() {
        return Collections.unmodifiableList(fieldErrors);
    }

    public ValidationException addFieldError(String field, String message) {
        return addFieldError(field, message, null);
    }

    /**
     * Adds a field-level validation error.
     * <p>
     * <b>Security note:</b> {@code rejectedValue} may be surfaced to API
     * clients or written to logs by downstream exception handlers.
     * Never pass sensitive values (passwords, tokens, secrets, PII) here.
     */
    public ValidationException addFieldError(String field, String message, Object rejectedValue) {
        Objects.requireNonNull(field, "field must not be null");
        Objects.requireNonNull(message, "message must not be null");
        this.fieldErrors.add(new FieldError(field, message, rejectedValue));
        return this;
    }

    @Override
    public String toString() {
        if (fieldErrors.isEmpty()) {
            return super.toString();
        }
        String errors = fieldErrors.stream()
                .map(fe -> fe.getField() + ": " + fe.getMessage())
                .collect(Collectors.joining("; "));
        return super.toString() + " [fieldErrors=" + errors + "]";
    }

    @Getter
    public static class FieldError implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String field;
        private final String message;
        private final transient Object rejectedValue;

        public FieldError(String field, String message) {
            this(field, message, null);
        }

        public FieldError(String field, String message, Object rejectedValue) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }
    }
}
