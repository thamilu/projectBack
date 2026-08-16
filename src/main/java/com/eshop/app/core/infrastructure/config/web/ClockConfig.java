package com.eshop.app.core.infrastructure.config.web;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes a standard java.time.Clock bean to enable time-deterministic unit testing across the
 * application services.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
