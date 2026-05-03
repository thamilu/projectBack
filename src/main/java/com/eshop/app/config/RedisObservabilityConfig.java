package com.eshop.app.config;

import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Observability beans for Redis: metrics integration and health indicator.
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.data.redis.repositories.enabled",
    havingValue = "true"
)
public class RedisObservabilityConfig {

    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources(MeterRegistry meterRegistry) {
        MicrometerOptions options = MicrometerOptions.builder()
            .histogram(true)
            .build();

        MicrometerCommandLatencyRecorder latencyRecorder = 
            new MicrometerCommandLatencyRecorder(meterRegistry, options);

        return DefaultClientResources.builder()
            .ioThreadPoolSize(4)
            .computationThreadPoolSize(4)
            .commandLatencyRecorder(latencyRecorder)
            .build();
    }

    // Health indicator is registered separately when Actuator is present.
    // To avoid a hard compile-time dependency on Spring Boot Actuator in environments
    // where it's intentionally excluded, the HealthIndicator bean is not declared here.
}
