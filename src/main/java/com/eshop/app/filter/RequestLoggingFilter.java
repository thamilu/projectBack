package com.eshop.app.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

/**
 * Request logging filter that uses ContentCaching wrappers to capture
 * request and response payloads and log them with configurable detail.
 */
@Component
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Value("${app.logging.request.max-payload-length:1000}")
    private int maxPayloadLength;

    @Value("${app.logging.request.include-headers:false}")
    private boolean includeHeaders;

    @Value("${app.logging.request.include-payload:false}")
    private boolean includePayload;

    @Value("${app.logging.request.enabled:true}")
    private boolean enabled;

    @Override
        protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain) throws ServletException, IOException {

            if (!enabled || shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, maxPayloadLength);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Instant start = Instant.now();
        if (log.isInfoEnabled()) {
            log.info("Request received: {} {}", request.getMethod(), request.getRequestURI());
        }
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            try {
                logRequestAndResponse(wrappedRequest, wrappedResponse, duration);
            } catch (Exception ex) {
                log.debug("Failed to log request/response: {}", ex.getMessage());
            }
            wrappedResponse.copyBodyToResponse();
        }
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator") || path.contains("/health") || path.contains("/swagger")
                || path.contains("/api-docs") || path.contains("/favicon.ico") || path.startsWith("/static/");
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       Duration duration) {
        StringBuilder sb = new StringBuilder();

        sb.append("\n===== HTTP REQUEST =====\n");
        sb.append(String.format("Method: %s\n", request.getMethod()));
        sb.append(String.format("URI: %s\n", request.getRequestURI()));
        sb.append(String.format("Query: %s\n", request.getQueryString()));
        sb.append(String.format("Client IP: %s\n", getClientIp(request)));

        if (includeHeaders) {
            sb.append("Headers:\n");
            Collections.list(request.getHeaderNames()).forEach(name -> {
                String value = "authorization".equalsIgnoreCase(name) ? "[REDACTED]" : request.getHeader(name);
                sb.append(String.format("  %s: %s\n", name, value));
            });
        }

        if (includePayload) {
            String reqBody = getPayload(request.getContentAsByteArray());
            if (!reqBody.isEmpty()) sb.append(String.format("Request Body: %s\n", sanitize(reqBody)));
        }

        sb.append("\n===== HTTP RESPONSE =====\n");
        sb.append(String.format("Status: %d\n", response.getStatus()));
        sb.append(String.format("Duration: %d ms\n", duration.toMillis()));

        if (includePayload) {
            String respBody = getPayload(response.getContentAsByteArray());
            if (!respBody.isEmpty()) sb.append(String.format("Response Body: %s\n", sanitize(respBody)));
        }

        sb.append("========================\n");

        if (response.getStatus() >= 400) log.warn(sb.toString()); else log.info(sb.toString());
    }

    private String getClientIp(HttpServletRequest request) {
        String[] headerNames = new String[]{"X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP"};
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                int idx = ip.indexOf(',');
                return idx != -1 ? ip.substring(0, idx).trim() : ip;
            }
        }
        return request.getRemoteAddr();
    }

    private String getPayload(byte[] content) {
        if (content == null || content.length == 0) return "";
        int len = Math.min(content.length, maxPayloadLength);
        String body = new String(content, 0, len, StandardCharsets.UTF_8);
        if (content.length > maxPayloadLength) body += "... [truncated]";
        return body;
    }

    private String sanitize(String input) {
        if (input == null) return null;
        return input.replaceAll("(?i)(password|token|secret|key|apikey|authorization)=[^&]*", "$1=***")
                .replaceAll("(?i)\"(password|token|secret|key|apikey)\"\\s*:\\s*\"[^\"]*\"", "\"$1\":\"***\"");
    }
}
