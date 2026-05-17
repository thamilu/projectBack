package com.eshop.app.core.observability.tracing;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

@Configuration
public class TracingConfig {

    @Bean
    public FilterRegistrationBean<TracingFilter> tracingFilter() {
        FilterRegistrationBean<TracingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new TracingFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    private static class TracingFilter implements Filter {
        private static final String HEADER_CORRELATION_ID = "X-Correlation-ID";

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String correlationId = httpRequest.getHeader(HEADER_CORRELATION_ID);
                TraceContext.start(correlationId);
            } else {
                TraceContext.start();
            }

            try {
                chain.doFilter(request, response);
            } finally {
                TraceContext.clear();
            }
        }
    }
}
