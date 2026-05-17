package com.eshop.app.core.ratelimit;

/**
 * [HARDEN] Rate limit key resolution strategy.
 * Determines how the rate limit key is coinputed per request.
 */
public enum RateLimitKeyType {
    /** Use client IP address (for public endpoints). */
    IP_ADDRESS,
    /** Use authenticated user ID (for user-specific limits). */
    USER,
    /** Use API key from X-API-Key header (for API consumers). */
    API_KEY,
    /** Single global key (for critical resource protection). */
    GLOBAL
}
