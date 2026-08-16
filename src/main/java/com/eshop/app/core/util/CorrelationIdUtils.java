package com.eshop.app.core.util;

import org.slf4j.MDC;

/**
 * Shared accessor for the request correlation ID stashed in MDC by
 * {@code CorrelationIdFilter}. Centralizes the MDC key and fallback value so
 * every exception-handling advice reads it identically.
 */
public final class CorrelationIdUtils {

    private static final String MDC_KEY = "correlationId";
    private static final String FALLBACK = "no-correlation-id";

    private CorrelationIdUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String getCurrent() {
        String value = MDC.get(MDC_KEY);
        return (value != null && !value.isBlank()) ? value : FALLBACK;
    }
}
