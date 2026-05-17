package com.eshop.app.core.ratelimit;

import com.eshop.app.core.exception.security.RateLimitExceededException;
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
 * [HARDEN] Rate Limiting Aspect.
 * Applies per-user, per-IP, or global rate limits via Resilience4j.
 * Lives in core/ratelimit as a shared cross-cutting governance concern.
 *
 * Rate limit key resolution:
 * - IP_ADDRESS: Client IP (for public endpoints)
 * - USER:       Authenticated user ID (for user-specific limits)
 * - API_KEY:    API key from header (for API consumers)
 * - GLOBAL:     Single global limit (for critical resources)
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingAspect {

    private final RateLimiterRegistry rateLimiterRegistry;

    @Around("@annotation(com.eshop.app.core.ratelimit.RateLimited)")
    public Object rateLimit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimited rateLimited = method.getAnnotation(RateLimited.class);

        if (rateLimited == null) {
            return joinPoint.proceed();
        }

        String rateLimiterName = rateLimited.value();
        String key = resolveRateLimitKey(rateLimited.keyType());
        String fullKey = rateLimiterName + ":" + key;

        RateLimiter limiter = rateLimiterRegistry.rateLimiter(fullKey, rateLimiterName);

        try {
            RateLimiter.waitForPermission(limiter);
            log.debug("Rate limit check passed: limiter={}, key={}", rateLimiterName, key);
            return joinPoint.proceed();
        } catch (RequestNotPermitted e) {
            log.warn("Rate limit exceeded: limiter={}, key={}, method={}", rateLimiterName, key, method.getName());
            throw new RateLimitExceededException(
                String.format("Rate limit exceeded for %s. Please try again later.", rateLimiterName),
                rateLimiterName,
                key
            );
        }
    }

    private String resolveRateLimitKey(RateLimitKeyType keyType) {
        return switch (keyType) {
            case IP_ADDRESS -> getClientIpAddress();
            case USER -> getCurrentUserId();
            case API_KEY -> getApiKey();
            case GLOBAL -> "global";
        };
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "anonymous";
    }

    private String getClientIpAddress() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
            if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
            return ip != null ? ip : "unknown";
        }
        return "unknown";
    }

    private String getApiKey() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            String apiKey = attrs.getRequest().getHeader("X-API-Key");
            return apiKey != null && !apiKey.isEmpty() ? apiKey : "no-api-key";
        }
        return "no-api-key";
    }
}


