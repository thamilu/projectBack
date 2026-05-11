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

        if (shouldSkip(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // BEST SPEED: Skip wrapping if general logging is disabled.
        // Note: You lose the request body for 500 errors, but you get maximum performance.
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, maxPayloadLength);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Instant start = Instant.now();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            Duration duration = Duration.between(start, Instant.now());
            
            // Log since enabled is true
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

    private void logRequestAndResponse(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Duration duration) {
        StringBuilder sb = new StringBuilder();

        String header = response.getStatus() >= 400 ? "HTTP ERROR LOG" : "HTTP REQUEST LOG";
        sb.append(String.format("\n===== %s =====\n", header));
        sb.append(String.format("Method: %s\n", request.getMethod()));
        sb.append(String.format("URI: %s\n", request.getRequestURI()));
        sb.append(String.format("Query: %s\n", request.getQueryString()));
        sb.append(String.format("Client IP: %s\n", getClientIp(request)));
        sb.append(String.format("Status: %d\n", response.getStatus()));
        sb.append(String.format("Duration: %d ms\n", duration.toMillis()));

        // Payload capture is only possible if wrappers were used
        if (request instanceof ContentCachingRequestWrapper wrappedRequest && includePayload) {
            String reqBody = getPayload(wrappedRequest.getContentAsByteArray());
            if (!reqBody.isEmpty()) sb.append(String.format("Request Body: %s\n", sanitize(reqBody)));
        }

        if (response instanceof ContentCachingResponseWrapper wrappedResponse && includePayload) {
            String respBody = getPayload(wrappedResponse.getContentAsByteArray());
            if (!respBody.isEmpty()) sb.append(String.format("Response Body: %s\n", sanitize(respBody)));
        }

        sb.append("========================\n");

        if (response.getStatus() >= 500) {
            log.error(sb.toString());
        } else if (response.getStatus() >= 400) {
            log.warn(sb.toString());
        } else {
            log.info(sb.toString());
        }
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
