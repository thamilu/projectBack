package com.eshop.app.core.exception.base;

/**
 * Thrown when an infrastructure-level failure occurs (e.g., database connectivity,
 * messaging broker, external service/API failure) as opposed to a business-rule
 * violation. Extends {@link AppException}, inheriting its non-null {@code errorCode}
 * guarantee and cause-preservation behavior.
 *
 * <p>Defaults {@code errorCode} to {@code "INFRASTRUCTURE_ERROR"} whenever no error
 * code is supplied, or {@code null} is explicitly passed.
 */
public class InfrastructureException extends AppException {
    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_ERROR_CODE = "INFRASTRUCTURE_ERROR";

    public InfrastructureException(String message) {
        super(message, DEFAULT_ERROR_CODE);
    }

    public InfrastructureException(String message, String errorCode) {
        super(message, errorCode != null ? errorCode : DEFAULT_ERROR_CODE);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, DEFAULT_ERROR_CODE, cause);
    }

    public InfrastructureException(String message, String errorCode, Throwable cause) {
        super(message, errorCode != null ? errorCode : DEFAULT_ERROR_CODE, cause);
    }
}
