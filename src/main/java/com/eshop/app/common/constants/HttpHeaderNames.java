package com.eshop.app.common.constants;

/**
 * HTTP header names used in the application.
 */
public final class HttpHeaderNames {
    
    public static final String CORRELATION_ID = "X-Correlation-ID";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String RETRY_AFTER = "Retry-After";
    public static final String AUTHORIZATION = "Authorization";
    
    private HttpHeaderNames() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}
