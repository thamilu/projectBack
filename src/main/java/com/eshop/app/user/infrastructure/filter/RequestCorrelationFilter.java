package com.eshop.app.user.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter propagating trace correlation IDs down the request lifecycle. Binds the traceId to MDC and
 * response headers.
 *
 * <p><b>Security note:</b> the inbound {@code X-Trace-Id} header is client-controlled and therefore
 * untrusted. It is validated against a safe character allow-list and maximum length before being
 * accepted; any value that fails validation is discarded and replaced with a freshly generated
 * UUID. This prevents log injection/forgery (CWE-117) and unbounded log/header content from
 * malicious or malformed client input.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    private static final String REQUEST_URI_MDC_KEY = "requestUri";

    private static final int MAX_TRACE_ID_LENGTH = 64;
    private static final Pattern SAFE_TRACE_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+$");

    /**
     * Resolves (or generates) a request trace ID, binds it and the request URI to the SLF4J MDC,
     * and echoes the trace ID back on the response for client-side correlation.
     *
     * <p>MDC is always cleared in a {@code finally} block to prevent leakage of request-scoped
     * diagnostic context onto threads reused by the container's thread pool.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String traceId = resolveTraceId(request.getHeader(TRACE_ID_HEADER));

        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(CORRELATION_ID_MDC_KEY, traceId);
        MDC.put(REQUEST_URI_MDC_KEY, request.getRequestURI());
        response.setHeader(TRACE_ID_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    /**
     * Validates a client-supplied trace ID against a safe character allow-list and maximum length.
     * Falls back to a freshly generated UUID when the header is absent, blank, or fails validation,
     * so malformed or malicious client input never reaches MDC, logs, or the response header.
     *
     * @param headerValue the raw {@code X-Trace-Id} header value, possibly {@code null}
     * @return a safe trace ID to use for this request
     */
    private String resolveTraceId(String headerValue) {
        return Optional.ofNullable(headerValue)
                .filter(id -> !id.isBlank())
                .filter(id -> id.length() <= MAX_TRACE_ID_LENGTH)
                .filter(id -> SAFE_TRACE_ID_PATTERN.matcher(id).matches())
                .orElseGet(() -> UUID.randomUUID().toString());
    }
}

