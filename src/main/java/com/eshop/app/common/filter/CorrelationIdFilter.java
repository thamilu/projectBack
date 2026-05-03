package com.eshop.app.common.filter;

import com.eshop.app.common.constants.HttpHeaderNames;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that adds correlation ID to all requests for tracing.
 * The correlation ID is:
 * 1. Read from X-Correlation-ID header if present
 * 2. Generated as new UUID if not present
 * 3. Added to MDC for logging
 * 4. Added to response headers
 */
@Component("commonCorrelationIdFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        
        String correlationId = request.getHeader(HttpHeaderNames.CORRELATION_ID);
        
        if (!StringUtils.hasText(correlationId)) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // Add to MDC for logging (will be included in log pattern)
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Add to response headers for client tracing
        response.setHeader(HttpHeaderNames.CORRELATION_ID, correlationId);
        
        try {
            chain.doFilter(request, response);
        } finally {
            // Clean up MDC to avoid memory leaks
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
