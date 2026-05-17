package com.eshop.app.core.infrastructure.config.resilience;

import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration(proxyBeanMethods = false)
@EnableRetry
public class RetryConfiguration {
    // Declarative retry enabled; add custom RetryTemplate beans here if needed
}
