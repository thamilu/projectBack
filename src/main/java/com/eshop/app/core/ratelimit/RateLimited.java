package com.eshop.app.core.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * [HARDEN] Rate Limiting Annotation.
 * Apply to controller methods to enable rate limiting via Resilience4j.
 *
 * Example:
 * <pre>
 * {@code @RateLimited(value = "analytics", keyType = RateLimitKeyType.USER)}
 * public AnalyticsDashboard getDashboard() { ... }
 * </pre>
 *
 * Lives in core/ratelimit as a shared governance concern.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {

    /**
     * Name of the Resilience4j rate limiter configuration to use.
     * Must match a configured limiter in application properties.
     * Available: public, authenticated, premium, admin, analytics, payment, upload
     */
    String value() default "authenticated";

    /** Key resolution strategy. */
    RateLimitKeyType keyType() default RateLimitKeyType.USER;
}
