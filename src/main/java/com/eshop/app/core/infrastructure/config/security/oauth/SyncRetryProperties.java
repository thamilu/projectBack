package com.eshop.app.core.infrastructure.config.security.oauth;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/** Configuration properties for externalizing User Identity Synchronization retry behavior. */
@Configuration
@ConfigurationProperties(prefix = "app.sync.retry")
@Validated
@Data
public class SyncRetryProperties {

    @Min(1)
    @Max(10)
    private int maxAttempts = 3;

    private BackoffProperties backoff = new BackoffProperties();

    /** Backoff configuration properties. */
    @Data
    public static class BackoffProperties {
        @Min(100)
        private long delay = 1000;

        @DecimalMin("1.0")
        private double multiplier = 2.0;

        @Min(1000)
        private long maxDelay = 5000;
    }
}
