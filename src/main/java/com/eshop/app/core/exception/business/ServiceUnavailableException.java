package com.eshop.app.core.exception.business;

import com.eshop.app.core.exception.base.BusinessException;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a service is temporarily unavailable (e.g. Identity resolution failure,
 * downstream dependency timeout, or circuit breaker open).
 * Always returns HTTP status 503 Service Unavailable, regardless of whether a root cause
 * or custom error code is supplied.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * throw new ServiceUnavailableException("Identity service is currently unavailable");
 *
 * // preserving the original failure:
 * try {
 *     identityClient.resolve(userId);
 * } catch (RestClientException ex) {
 *     throw new ServiceUnavailableException(
 *             "Identity service is currently unavailable", "IDENTITY_SERVICE_TIMEOUT", ex);
 * }
 * }</pre>
 */
public class ServiceUnavailableException extends BusinessException {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "SERVICE_UNAVAILABLE";

    public ServiceUnavailableException(String message) {
        super(requireNonBlank(message), DEFAULT_ERROR_CODE, HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String message, String errorCode) {
        super(requireNonBlank(message), resolveErrorCode(errorCode), HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(requireNonBlank(message), DEFAULT_ERROR_CODE, HttpStatus.SERVICE_UNAVAILABLE, cause);
    }

    public ServiceUnavailableException(String message, String errorCode, Throwable cause) {
        super(requireNonBlank(message), resolveErrorCode(errorCode), HttpStatus.SERVICE_UNAVAILABLE, cause);
    }

    private static String resolveErrorCode(String errorCode) {
        return errorCode != null ? errorCode : DEFAULT_ERROR_CODE;
    }

    private static String requireNonBlank(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
        return message;
    }
}
