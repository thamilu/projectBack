package com.eshop.app.exception;

import lombok.Getter;

/**
 * Exception thrown when rate limit is exceeded.
 * 
 * @since 2.0
 */
@Getter
public class RateLimitExceededException extends RuntimeException {
    
    private final String limiterName;
    private final String key;
    
    public RateLimitExceededException(String message) {
        super(message);
        this.limiterName = "unknown";
        this.key = "unknown";
    }
    
    public RateLimitExceededException(String message, String limiterName, String key) {
        super(message);
        this.limiterName = limiterName;
        this.key = key;
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, cause);
        this.limiterName = "unknown";
        this.key = "unknown";
    }
}
