package com.eshop.app.core.infrastructure.config.security.web;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.kernel.ApiConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class SecurityHeadersConfig {

    private final AppProperties appProperties;

    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new SecurityHeadersFilter(appProperties));
        registration.addUrlPatterns(ApiConstants.BASE_PATH + "/*");
        registration.setOrder(1);
        return registration;
    }
}
