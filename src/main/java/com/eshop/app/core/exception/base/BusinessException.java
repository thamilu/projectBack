package com.eshop.app.core.exception.base;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Base exception for all business-rule violations. Extends {@link AppException},
 * relying on it as the single source of truth for {@code errorCode} (always non-null;
 * see {@link AppException#getErrorCode()}).
 *
 * <p>Default {@code httpStatus} behavior by constructor:
 * <ul>
 *   <li>No {@link HttpStatus} and no cause supplied → {@code 400 BAD_REQUEST}.</li>
 *   <li>No {@link HttpStatus} but a cause is supplied → {@code 500 INTERNAL_SERVER_ERROR}
 *       (an unexpected/wrapped failure is assumed server-side unless a status is stated).</li>
 *   <li>Explicit {@link HttpStatus} passed as {@code null} → defaults to
 *       {@code 500 INTERNAL_SERVER_ERROR} (fail-safe; never exposes a null status to
 *       response-building code).</li>
 * </ul>
 *
 * <p>{@code args} is reserved for downstream message interpolation (e.g., i18n resolution
 * of {@code errorCode} via a {@code MessageSource} in the global exception handler); it is
 * not consumed by this class.
 */
@Getter
public class BusinessException extends AppException {
    private static final long serialVersionUID = 1L;

    private final HttpStatus httpStatus;
    private final Map<String, Object> details = new HashMap<>();
    private final Object[] args;

    public BusinessException(String message) {
        super(message, "BUSINESS_ERROR");
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.args = null;
    }

    public BusinessException(String message, String errorCode) {
        super(message, errorCode);
        this.httpStatus = HttpStatus.BAD_REQUEST;
        this.args = null;
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus) {
        super(message, errorCode);
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.args = null;
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Object[] args) {
        super(message, errorCode);
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.args = args;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, "BUSINESS_ERROR", cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.args = null;
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        this.args = null;
    }

    public BusinessException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, errorCode, cause);
        this.httpStatus = httpStatus != null ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR;
        this.args = null;
    }

    public BusinessException addDetail(String key, Object value) {
        this.details.put(key, value);
        return this;
    }

    public Map<String, Object> getDetails() {
        return new HashMap<>(details);
    }

    public Object[] getArgs() {
        return args != null ? args.clone() : null;
    }
}
