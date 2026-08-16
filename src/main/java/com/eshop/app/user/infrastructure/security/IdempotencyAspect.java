package com.eshop.app.user.infrastructure.security;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.user.application.security.IdempotentOperation;
import com.eshop.app.user.application.service.IdempotencyKeyService;
import com.eshop.app.user.application.service.IdempotencyKeyService.IdempotencyResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Aspect handling request deduplication and response caching for API endpoints annotated
 * with @IdempotentOperation.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyAspect {

    private final IdempotencyKeyService idempotencyKeyService;
    private final ObjectMapper objectMapper;

    @Around(
            "@annotation(idempotentOperation) &&"
                    + " @annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object enforceIdempotency(
            ProceedingJoinPoint joinPoint, IdempotentOperation idempotentOperation)
            throws Throwable {

        String idempotencyKey = extractIdempotencyKey();

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed(); // No key = pass through
        }

        String operationType = idempotentOperation.operationType();

        // Check cached status or reserve slot atomically via Redis Lua script
        IdempotencyResult claimResult =
                idempotencyKeyService.checkAndClaim(idempotencyKey, operationType);

        if (claimResult instanceof IdempotencyResult.CachedResult cached) {
            return handleCachedResult(
                    cached.serializedValue(),
                    idempotentOperation.resultType(),
                    idempotencyKey,
                    operationType);
        } else if (claimResult instanceof IdempotencyResult.Processing) {
            throw new BusinessException(
                    "Request is currently being processed",
                    "IDEMPOTENCY_PROCESSING",
                    HttpStatus.CONFLICT);
        }

        // FirstExecution
        try {
            Object result = joinPoint.proceed();
            cacheResult(idempotencyKey, operationType, result);
            return result;
        } catch (Throwable t) {
            // Delete reservation so subsequent client retries can proceed
            idempotencyKeyService.clearKey(idempotencyKey, operationType);
            throw t;
        }
    }

    private Object handleCachedResult(
            String cachedValue, Class<?> resultType, String idempotencyKey, String operationType) {

        if ("PROCESSING".equals(cachedValue)) {
            throw new BusinessException(
                    "Request is currently being processed",
                    "IDEMPOTENCY_PROCESSING",
                    HttpStatus.CONFLICT);
        }

        try {
            // Deserialize response into ResponseEntity<ApiResponse<T>>
            JavaType responseType =
                    objectMapper
                            .getTypeFactory()
                            .constructParametricType(
                                    ResponseEntity.class,
                                    objectMapper
                                            .getTypeFactory()
                                            .constructParametricType(
                                                    ApiResponse.class, resultType));

            return objectMapper.readValue(cachedValue, responseType);
        } catch (JsonProcessingException e) {
            log.error(
                    "Idempotency cache deserialization failed for key={} op={}: {}",
                    idempotencyKey,
                    operationType,
                    e.getMessage());
            // Fail fast, prevent silent re-execution of corrupted cache entries
            throw new BusinessException(
                    "Idempotency cache corrupted. Please retry with a new key.",
                    "IDEMPOTENCY_CACHE_CORRUPT",
                    HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    private void cacheResult(String key, String operationType, Object result) {
        try {
            idempotencyKeyService.storeResult(
                    key, operationType, objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException e) {
            log.error("Failed to cache idempotency result for key={}: {}", key, e.getMessage());
        }
    }

    private String extractIdempotencyKey() {
        return Optional.ofNullable(
                        ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()))
                .map(attrs -> attrs.getRequest().getHeader("Idempotency-Key"))
                .orElse(null);
    }
}
