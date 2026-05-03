package com.eshop.app.aspect;

import com.eshop.app.exception.RateLimitExceededException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Rate Limiting Aspect
 * <p>
 * Applies rate limiting to methods annotated with @RateLimited.
 * Uses Resilience4j for efficient, thread-safe rate limiting.
 * <p>
 * Rate limit key is determined by:
 * <ul>
 *   <li>IP_ADDRESS: Client IP (for public endpoints)</li>
 *   <li>USER: Authenticated user ID (for user-specific limits)</li>
 *   <li>API_KEY: API key from header (for API consumers)</li>
 *   <li>GLOBAL: Single global limit (for critical resources)</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingAspect {
    
    private final RateLimiterRegistry rateLimiterRegistry;
    
    @Around("@annotation(com.eshop.app.validation.RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        com.eshop.app.validation.RateLimited rateLimited = method.getAnnotation(com.eshop.app.validation.RateLimited.class);
        
        if (rateLimited == null) {
            return joinPoint.proceed();
        }
        
        String rateLimiterName = rateLimited.value();
        String key = resolveRateLimitKey(rateLimited.keyType());
        String fullKey = rateLimiterName + ":" + key;
        
        // Get or create rate limiter for this specific key
        RateLimiter limiter = rateLimiterRegistry.rateLimiter(fullKey, rateLimiterName);
        
        try {
            // Attempt to acquire permission
            RateLimiter.waitForPermission(limiter);
            
            log.debug("Rate limit check passed: limiter={}, key={}", rateLimiterName, key);
            return joinPoint.proceed();
            
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded: limiter={}, key={}, method={}", 
                rateLimiterName, key, method.getName());
            
            throw new RateLimitExceededException(
                String.format("Rate limit exceeded for %s. Please try again later.", rateLimiterName),
                rateLimiterName,
                key
            );
        }
    }
    
    /**
     * Resolve the rate limit key based on the key type
     */
    private String resolveRateLimitKey(com.eshop.app.validation.RateLimitKeyType keyType) {
        return switch (keyType) {
            case IP_ADDRESS -> getClientIpAddress();
            case USER -> getCurrentUserId();
            case API_KEY -> getApiKey();
            case GLOBAL -> "global";
        };
    }
    
    /**
     * Get current authenticated user ID
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() 
            && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "anonymous";
    }
    
    /**
     * Get client IP address
     */
    private String getClientIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            // Handle X-Forwarded-For with multiple IPs
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip != null ? ip : "unknown";
        }
        return "unknown";
    }
    
    /**
     * Get API key from request header
     */
    private String getApiKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String apiKey = request.getHeader("X-API-Key");
            return apiKey != null && !apiKey.isEmpty() ? apiKey : "no-api-key";
        }
        return "no-api-key";
    }
}
