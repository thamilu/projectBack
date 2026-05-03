package com.eshop.app.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * HIGH-003 FIX: Pagination Limit Enforcement Aspect
 * 
 * <p>Prevents unbounded pagination that can cause memory exhaustion and DoS attacks.
 * Automatically enforces maximum page size limits on all controller methods
 * accepting Pageable parameters.
 * 
 * <h2>Protection Mechanisms:</h2>
 * <ul>
 *   <li>Maximum page size: 500 (configurable via properties)</li>
 *   <li>Default page size: 20 (when unpaged or size = 0)</li>
 *   <li>Automatic request parameter sanitization</li>
 *   <li>Logging of limit enforcement for security monitoring</li>
 * </ul>
 * 
 * <h2>Performance Impact:</h2>
 * <ul>
 *   <li>Prevents: 100,000 products × 2KB = 200MB per request</li>
 *   <li>Reduces: OOM errors under malicious load</li>
 *   <li>Space Complexity: O(N) where N ≤ MAX_PAGE_SIZE (bounded)</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * <pre>
 * # application.properties
 * pagination.max-page-size=500
 * pagination.default-page-size=20
 * </pre>
 * 
 * @author EShop Security Team
 * @version 1.0
 * @since 2025-12-20
 */
@Aspect
@Component
@Slf4j
public class PaginationLimitAspect {
    
    /**
     * Maximum allowed page size to prevent memory exhaustion.
     * Default: 500 items per page
     */
    @Value("${pagination.max-page-size:500}")
    private int maxPageSize;
    
    /**
     * Default page size when unpaged or zero-size requests are made.
     * Default: 20 items per page
     */
    @Value("${pagination.default-page-size:20}")
    private int defaultPageSize;
    
    /**
     * Intercepts all controller methods with Pageable parameters.
     * Enforces maximum page size and sets defaults for unpaged requests.
     * 
     * @param joinPoint the intercepted method execution
     * @return the result of the method execution with sanitized pagination
     * @throws Throwable if the underlying method throws an exception
     */
    @Around("execution(* com.eshop.app.controller..*(..)) && args(..,pageable)")
    public Object enforcePaginationLimits(ProceedingJoinPoint joinPoint, Pageable pageable) throws Throwable {
        
        if (pageable == null) {
            // No pagination requested, use default
            log.debug("No pagination specified, using default: page=0, size={}", defaultPageSize);
            return joinPoint.proceed(
                replaceArgument(joinPoint.getArgs(), PageRequest.of(0, defaultPageSize))
            );
        }
        
        // Handle unpaged requests
        if (pageable.isUnpaged()) {
            log.warn("Unpaged request detected, enforcing default pagination: size={}", defaultPageSize);
            Pageable bounded = PageRequest.of(0, defaultPageSize, pageable.getSort());
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(), bounded));
        }
        
        // Check if page size exceeds maximum
        int requestedSize = pageable.getPageSize();
        if (requestedSize > maxPageSize) {
            log.warn(
                "⚠️  Page size {} exceeds maximum {}, enforcing limit. Method: {}",
                requestedSize, maxPageSize, joinPoint.getSignature().toShortString()
            );
            
            Pageable bounded = PageRequest.of(
                pageable.getPageNumber(),
                maxPageSize,
                pageable.getSort()
            );
            
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(), bounded));
        }
        
        // Check for zero or negative page size
        if (requestedSize <= 0) {
            log.warn("Invalid page size {} detected, using default: {}", requestedSize, defaultPageSize);
            Pageable bounded = PageRequest.of(
                pageable.getPageNumber(),
                defaultPageSize,
                pageable.getSort()
            );
            
            return joinPoint.proceed(replaceArgument(joinPoint.getArgs(), bounded));
        }
        
        // Page size is valid, proceed normally
        return joinPoint.proceed();
    }
    
    /**
     * Replaces the Pageable argument in the method arguments array.
     * 
     * @param args original method arguments
     * @param newPageable sanitized pageable to use
     * @return modified arguments array
     */
    private Object[] replaceArgument(Object[] args, Pageable newPageable) {
        Object[] modifiedArgs = new Object[args.length];
        System.arraycopy(args, 0, modifiedArgs, 0, args.length);
        
        // Find and replace Pageable argument
        for (int i = 0; i < modifiedArgs.length; i++) {
            if (modifiedArgs[i] instanceof Pageable) {
                modifiedArgs[i] = newPageable;
                break;
            }
        }
        
        return modifiedArgs;
    }
}
