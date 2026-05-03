package com.eshop.app.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate Limiting Annotation
 * <p>
 * Apply to controller methods to enable rate limiting.
 * <p>
 * Example usage:
 * <pre>
 * {@code @RateLimited(value = "analytics", keyType = RateLimitKeyType.USER)}
 * public AnalyticsDashboard getDashboard() {
 *     return analyticsService.getDashboard();
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    
    /**
     * Name of the rate limiter configuration to use.
     * Must match a configuration defined in RateLimitConfiguration.
     * <p>
     * Available options: public, authenticated, premium, admin, analytics, payment, upload
     */
    String value() default "authenticated";
    
    /**
     * Type of key to use for rate limiting
     */
    RateLimitKeyType keyType() default RateLimitKeyType.USER;
}
