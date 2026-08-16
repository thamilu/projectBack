package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a method or request parameter fails validation (e.g., malformed format,
 * out-of-range value, or unparseable input).
 *
 * <p>Extends {@link BusinessException} and always maps to HTTP {@code 400 Bad Request},
 * regardless of whether a root cause is supplied.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * throw new InvalidParameterException("page must be a positive integer");
 *
 * // preserving the original parsing failure:
 * try {
 *     Integer.parseInt(pageParam);
 * } catch (NumberFormatException ex) {
 *     throw new InvalidParameterException("page must be a valid integer: " + pageParam, ex);
 * }
 * }</pre>
 */
public class InvalidParameterException extends BusinessException {

    private static final String ERROR_CODE = "INVALID_PARAMETER";

    public InvalidParameterException(String message) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super(message, ERROR_CODE, HttpStatus.BAD_REQUEST, cause);
    }
}
