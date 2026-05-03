package com.eshop.app.config.resilience;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Configuration - Enterprise Grade Fault Tolerance
 * 
 * <h2>HIGH-004 FIX: Service-Specific Circuit Breaker Configurations</h2>
 * <ul>
 *   <li>Payment Gateway: Conservative (70% threshold, 2min recovery)</li>
 *   <li>External APIs: Moderate (60% threshold, 30s recovery)</li>
 *   <li>Internal Services: Aggressive (50% threshold, 10s recovery)</li>
 * </ul>
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Rate Limiting - Prevent API abuse and overload</li>
 *   <li>Bulkhead - Isolate concurrent operations</li>
 *   <li>Circuit Breaker - Handle cascading failures</li>
 *   <li>Retry - Handle transient failures gracefully</li>
 * </ul>
 * 
 * <h2>Patterns:</h2>
 * <ul>
 *   <li>Dashboard: 100 req/min, 50 concurrent</li>
 *   <li>Analytics: 20 req/min, 25 concurrent</li>
 *   <li>Circuit Breaker: Service-specific configurations</li>
 * </ul>
 * 
 * @author EShop Team
 * @version 2.1
 * @since 2025-12-15
 */
@Configuration
@Slf4j
public class Resilience4jConfig {
    
    // Rate Limiter Registry is now defined in RateLimitConfiguration.java
    // to provide comprehensive tiered rate limiting (7 tiers vs 3 tiers)
    
    /**
     * Bulkhead Registry for Concurrency Control
     */
    @Bean
    public BulkheadRegistry bulkheadRegistry() {
        log.info("Configuring Resilience4j Bulkheads");
        
        // Dashboard Bulkhead (50 concurrent calls, 1s wait)
        BulkheadConfig dashboardConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(50)
            .maxWaitDuration(Duration.ofSeconds(1))
            .build();
        
        // Analytics Bulkhead (25 concurrent calls, 500ms wait)
        BulkheadConfig analyticsConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(25)
            .maxWaitDuration(Duration.ofMillis(500))
            .build();
        
        // Default Bulkhead (20 concurrent calls, 500ms wait)
        BulkheadConfig defaultConfig = BulkheadConfig.custom()
            .maxConcurrentCalls(20)
            .maxWaitDuration(Duration.ofMillis(500))
            .build();
        
        BulkheadRegistry registry = BulkheadRegistry.of(defaultConfig);
        registry.bulkhead("dashboard", dashboardConfig);
        registry.bulkhead("analytics", analyticsConfig);
        
        log.info("Bulkheads configured: dashboard (50 concurrent), analytics (25 concurrent)");
        return registry;
    }
    
    /**
     * HIGH-004 FIX: Circuit Breaker Registry with environment-specific configurations.
     * 
     * Strategy:
     * - Payment Gateway: Conservative (70% threshold, 2min recovery)
     * - External APIs: Moderate (60% threshold, 30s recovery)
     * - Internal Services: Aggressive (50% threshold, 10s recovery)
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        log.info("Configuring Resilience4j Circuit Breakers");
        
        // Payment Gateway Circuit Breaker - CONSERVATIVE for financial operations
        CircuitBreakerConfig paymentConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(70)  // Higher threshold - tolerate more failures
            .slowCallRateThreshold(60)  // 60% slow calls trigger circuit
            .slowCallDurationThreshold(Duration.ofSeconds(5))  // Payment APIs can be slow
            .waitDurationInOpenState(Duration.ofMinutes(2))  // Longer recovery time
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED)
            .slidingWindowSize(100)  // 100 seconds of data
            .minimumNumberOfCalls(20)  // Need statistical significance
            .permittedNumberOfCallsInHalfOpenState(5)  // Test with 5 calls
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .recordExceptions(
                java.io.IOException.class,
                java.util.concurrent.TimeoutException.class,
                org.springframework.web.client.ResourceAccessException.class
            )
            .ignoreExceptions(
                jakarta.validation.ValidationException.class,
                IllegalArgumentException.class,
                com.eshop.app.exception.ValidationException.class
            )
            .build();
        
        // External API Circuit Breaker - MODERATE
        CircuitBreakerConfig externalApiConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(60)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(3))
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(50)
            .minimumNumberOfCalls(10)
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();
        
        // Internal Service Circuit Breaker - AGGRESSIVE (fail fast)
        CircuitBreakerConfig internalConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(40)
            .slowCallDurationThreshold(Duration.ofSeconds(1))
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .slidingWindowSize(20)
            .minimumNumberOfCalls(5)
            .permittedNumberOfCallsInHalfOpenState(2)
            .build();
        
        // Default Circuit Breaker
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .slidingWindowSize(30)
            .build();
        
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(defaultConfig);
        
        // Register named circuit breakers
        registry.circuitBreaker("paymentGateway", paymentConfig);
        registry.circuitBreaker("externalApi", externalApiConfig);
        registry.circuitBreaker("internalService", internalConfig);
        registry.circuitBreaker("cloudinary", externalApiConfig);  // Image upload
        registry.circuitBreaker("cdn", externalApiConfig);  // CDN operations
        
        // Add event listeners for monitoring and alerting
        addCircuitBreakerEventListeners(registry.circuitBreaker("paymentGateway"), "PaymentGateway");
        addCircuitBreakerEventListeners(registry.circuitBreaker("externalApi"), "ExternalAPI");
        
        log.info("Circuit breakers configured: paymentGateway (70%, 2min), externalApi (60%, 30s), internalService (50%, 10s)");
        return registry;
    }
    
    /**
     * Adds event listeners to circuit breaker for monitoring.
     */
    private void addCircuitBreakerEventListeners(
            io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker, 
            String name) {
        
        circuitBreaker.getEventPublisher()
            .onStateTransition(event -> 
                log.warn("[{}] Circuit breaker state changed: {} -> {}", 
                    name,
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState())
            )
            .onFailureRateExceeded(event ->
                log.error("[{}] Circuit breaker failure rate exceeded: {}%", 
                    name, event.getFailureRate())
            )
            .onSlowCallRateExceeded(event ->
                log.warn("[{}] Circuit breaker slow call rate exceeded: {}%", 
                    name, event.getSlowCallRate())
            )
            .onError(event ->
                log.debug("[{}] Circuit breaker recorded error: {}", 
                    name, event.getThrowable().getClass().getSimpleName())
            );
    }
    
    /**
     * Retry Registry for Transient Failures
     */
    @Bean
    public RetryRegistry retryRegistry() {
        log.info("Configuring Resilience4j Retry");
        
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(500))
            .retryExceptions(
                java.net.SocketTimeoutException.class,
                org.springframework.dao.TransientDataAccessException.class
            )
            .build();
        
        RetryRegistry registry = RetryRegistry.of(config);
        log.info("Retry configured: max 3 attempts, 500ms wait");
        return registry;
    }
}
