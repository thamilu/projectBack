package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.util.StringUtils;

/**
 * Applies the security headers configured under {@code app.security.headers} (see
 * {@link AppProperties.Security.Headers}). Values are read from configuration rather than
 * hardcoded so HSTS/CSP can be tuned per environment without a code change.
 */
public class SecurityHeadersFilter extends HttpFilter {

    private final AppProperties appProperties;

    public SecurityHeadersFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        AppProperties.Security.Headers headers = appProperties.getSecurity().getHeaders();
        if (headers.isEnabled()) {
            response.setHeader("X-Content-Type-Options", headers.getXContentTypeOptions());
            response.setHeader("X-Frame-Options", headers.getXFrameOptions());
            response.setHeader("X-XSS-Protection", headers.getXXssProtection());
            response.setHeader("Referrer-Policy", headers.getReferrerPolicy());
            if (StringUtils.hasText(headers.getStrictTransportSecurity())) {
                response.setHeader("Strict-Transport-Security", headers.getStrictTransportSecurity());
            }
            if (StringUtils.hasText(headers.getContentSecurityPolicy())) {
                response.setHeader("Content-Security-Policy", headers.getContentSecurityPolicy());
            }
        }

        chain.doFilter(request, response);
    }
}
