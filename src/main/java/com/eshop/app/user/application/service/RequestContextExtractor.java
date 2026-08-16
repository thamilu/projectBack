package com.eshop.app.user.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
public class RequestContextExtractor {

    /** Resolves the original client IP address from request headers or remote address. */
    public String extractClientIp(ServerWebExchange exchange) {
        if (exchange == null) {
            return "unknown";
        }

        // Check X-Forwarded-For (if behind proxy/load balancer)
        List<String> forwardedFor = exchange.getRequest().getHeaders().get("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            String firstIp = forwardedFor.get(0);
            if (firstIp != null && !firstIp.isBlank()) {
                return firstIp.split(",")[0].trim();
            }
        }

        // Check X-Real-IP
        String realIp = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        // Fallback to remote address
        return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                .map(addr -> addr.getAddress().getHostAddress())
                .orElse("unknown");
    }

    /** Resolves the request User-Agent header. */
    public String extractUserAgent(ServerWebExchange exchange) {
        if (exchange == null) {
            return "unknown";
        }
        return Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("User-Agent"))
                .filter(ua -> !ua.isBlank())
                .orElse("unknown");
    }

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_ATTR = "correlationId";

    /** Resolves or generates the correlation ID from the request attributes or headers. */
    public String extractCorrelationId(ServerWebExchange exchange) {
        if (exchange == null) {
            return UUID.randomUUID().toString();
        }
        // First try exchange attributes (set by filter)
        Object attr = exchange.getAttributes().get(CORRELATION_ID_ATTR);
        if (attr instanceof String) {
            return (String) attr;
        }
        // Fallback to header
        String header = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        return header != null && !header.isBlank() ? header : UUID.randomUUID().toString();
    }
}
