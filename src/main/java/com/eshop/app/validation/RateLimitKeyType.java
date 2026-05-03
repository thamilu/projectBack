package com.eshop.app.validation;

/**
 * Rate Limit Key Type
 * <p>
 * Determines how the rate limit key is resolved for tracking limits.
 */
public enum RateLimitKeyType {
    /**
     * Use client IP address as key (for public endpoints)
     */
    IP_ADDRESS,
    
    /**
     * Use authenticated user ID as key (for user-specific limits)
     */
    USER,
    
    /**
     * Use API key from header as key (for API consumers)
     */
    API_KEY,
    
    /**
     * Use a single global key (for critical resources)
     */
    GLOBAL
}
