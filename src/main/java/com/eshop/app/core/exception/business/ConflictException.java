package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a business operation conflicts with the current state of a resource
 * (e.g., duplicate resource creation, unique constraint violation, optimistic locking
 * conflict). Maps to HTTP {@code 409 Conflict} via the {@code GlobalExceptionHandler}.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * throw new ConflictException("Email already registered: " + email);
 * throw new ConflictException("Unique constraint violation", dataIntegrityViolationException);
 * }</pre>
 */
public class ConflictException extends BusinessException {

    private static final String ERROR_CODE = "CONFLICT";

    /**
     * Constructs a conflict exception with a descriptive message and HTTP 409 status.
     *
     * @param message human-readable description of the resource conflict
     */
    public ConflictException(String message) {
        super(message, ERROR_CODE, HttpStatus.CONFLICT);
    }

    /**
     * Constructs a conflict exception with a message, cause, and HTTP 409 status.
     *
     * @param message human-readable description of the resource conflict
     * @param cause   underlying cause (e.g., DataIntegrityViolationException)
     */
    public ConflictException(String message, Throwable cause) {
        super(message, ERROR_CODE, HttpStatus.CONFLICT, cause);
    }
}
