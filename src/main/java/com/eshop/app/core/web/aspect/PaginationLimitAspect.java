package com.eshop.app.core.web.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * [HARDEN] Pagination Limit Enforcement Aspect.
 *
 * Prevents memory exhaustion (OOM) and DoS attacks via unbounded pagination.
 * Automatically enforces max page size limits on all controller methods.
 * Lives in core/web/aspect as a reusable cross-cutting concern.
 *
 * Configuration:
 *   pagination.max-page-size=500
 *   pagination.default-page-size=20
 */
@Aspect
@Component
@Slf4j
public class PaginationLimitAspect {

    @Value("${pagination.max-page-size:500}")
    private int maxPageSize;

    @Value("${pagination.default-page-size:20}")
    private int defaultPageSize;

    @Around("execution(* com.eshop.app..api.controller..*(..)) && args(..,pageable)")
    public Object enforcePaginationLimits(ProceedingJoinPoint joinPoint, Pageable pageable) throws Throwable {

        if (pageable == null) {
            log.debug("No pagination specified, using default: page=0, size={}", defaultPageSize);
            return joinPoint.proceed(
                replaceArgument(joinPoint.getArgs(), PageRequest.of(0, defaultPageSize))
            );
        }

        if (pageable.isUnpaged()) {
            log.warn("Unpaged request detected, enforcing default pagination: size={}", defaultPageSize);
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(),
                PageRequest.of(0, defaultPageSize, pageable.getSort())));
        }

        int requestedSize = pageable.getPageSize();
        if (requestedSize > maxPageSize) {
            log.warn("Page size {} exceeds maximum {}, capping. Method: {}",
                requestedSize, maxPageSize, joinPoint.getSignature().toShortString());
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(),
                PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort())));
        }

        if (requestedSize <= 0) {
            log.warn("Invalid page size {}, using default: {}", requestedSize, defaultPageSize);
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(),
                PageRequest.of(pageable.getPageNumber(), defaultPageSize, pageable.getSort())));
        }

        return joinPoint.proceed();
    }

    private Object[] replaceArgument(Object[] args, Pageable newPageable) {
        Object[] modifiedArgs = new Object[args.length];
        System.arraycopy(args, 0, modifiedArgs, 0, args.length);
        for (int i = 0; i < modifiedArgs.length; i++) {
            if (modifiedArgs[i] instanceof Pageable) {
                modifiedArgs[i] = newPageable;
                break;
            }
        }
        return modifiedArgs;
    }
}
