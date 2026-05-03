package com.eshop.app.config;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class GracefulShutdownConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownConfiguration.class);

    @PreDestroy
    public void onShutdown() {
        log.info("Application shutdown initiated - completing in-flight requests");
    }
}
