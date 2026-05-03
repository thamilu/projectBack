package com.eshop.app.config;



import org.springframework.context.annotation.Configuration;



@Configuration
public class ResilienceConfig {



        // All registry beans are now provided by Resilience4jConfig. Inject them here if needed.
        // Example:
        // private final CircuitBreakerRegistry circuitBreakerRegistry;
        // private final RateLimiterRegistry rateLimiterRegistry;
        // private final BulkheadRegistry bulkheadRegistry;
        //
        // @Autowired or @RequiredArgsConstructor
        // public ResilienceConfig(CircuitBreakerRegistry circuitBreakerRegistry, ... ) { ... }
        //
        // Define only consumer beans here, e.g.:
        // @Bean
        // public CircuitBreaker paymentCircuitBreaker() {
        //     return circuitBreakerRegistry.circuitBreaker("payments");
        // }
}
