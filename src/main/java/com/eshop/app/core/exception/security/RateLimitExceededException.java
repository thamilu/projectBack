package com.eshop.app.core.exception.security;

import com.eshop.app.core.exception.base.BusinessException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends BusinessException {
    private final String limiterName;
    private final String key;
    
    public RateLimitExceededException(String message) {
        super(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
        this.limiterName = "unknown";
        this.key = "unknown";
    }
    
    public RateLimitExceededException(String message, String limiterName, String key) {
        super(message, "RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS);
        this.limiterName = limiterName;
        this.key = key;
    }
    
    public RateLimitExceededException(String message, Throwable cause) {
        super(message, "RATE_LIMIT_EXCEEDED", cause);
        this.limiterName = "unknown";
        this.key = "unknown";
    }
}
