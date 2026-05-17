package com.eshop.app.core.outbox;
import com.eshop.app.core.exception.base.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;

/**
 * Servlet interceptor enforcing idempotency on all mutating HTTP operations.
 *
 * <p>Clients must include an {@code X-Idempotency-Key} header on POST/PUT/PATCH
 * requests. The key is stored in Redis for 24 hours. Duplicate requests with the
 * same key are rejected with HTTP 409 (Conflict), preventing accidental double
 * submissions for critical operations like payments and order creation.
 *
 * <p>Part of the Enterprise Core Outbox/Idempotency Layer.
 *
 * @since 2.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotencyInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private static final String REDIS_PREFIX = "idempotency:";
    private static final Duration KEY_TTL = Duration.ofHours(24);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();

        // Only apply to mutating operations (safe methods are idempotent by definition)
        if (!"POST".equalsIgnoreCase(method)
                && !"PUT".equalsIgnoreCase(method)
                && !"PATCH".equalsIgnoreCase(method)) {
            return true;
        }

        String key = request.getHeader(IDEMPOTENCY_HEADER);
        if (key == null || key.isBlank()) {
            log.warn("Missing X-Idempotency-Key for {} {}", method, request.getRequestURI());
            return true; // Soft enforcement: warn only; harden to reject if required
        }

        String redisKey = REDIS_PREFIX + key;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", KEY_TTL);

        if (Boolean.FALSE.equals(isNew)) {
            log.error("Duplicate request detected with idempotency key: {}", key);
            throw new BusinessException(
                    "Duplicate request detected. This operation has already been processed or is in progress.",
                    "DUPLICATE_REQUEST",
                    HttpStatus.CONFLICT
            );
        }

        log.debug("Accepted idempotency key: {}", key);
        return true;
    }
}

