package com.eshop.app.aspect;

import com.eshop.app.config.PaginationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * AOP aspect for automatic pageable validation.
 * Intercepts controller methods to sanitize and validate pagination parameters.
 *
 * @author E-Shop Team
 * @since 2.0.0
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PageableValidationAspect {
    
    private final PaginationProperties paginationProperties;
    
    // Whitelist of allowed sort properties
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
        "id", "name", "price", "createdAt", "updatedAt", 
        "stockQuantity", "sku", "featured", "active"
    );
    
    private static final int MAX_SORT_PROPERTIES = 3;
    
    /**
     * Intercepts methods with Pageable parameter and validates/sanitizes it.
     */
    @Around("execution(* com.eshop.app.controller..*(.., org.springframework.data.domain.Pageable))")
    public Object validatePageable(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Pageable p) {
                Pageable sanitized = sanitizePageable(p);
                args[i] = sanitized;
                log.debug("Sanitized pageable: page={}, size={}, sort= {}",
                    sanitized.getPageNumber(), sanitized.getPageSize(), sanitized.getSort());
            }
        }
        return joinPoint.proceed(args);
    }
    
    /**
     * Sanitizes a Pageable object by enforcing size limits and validating sort properties.
     */
    private Pageable sanitizePageable(Pageable pageable) {
        int maxSize = paginationProperties.getMaxPageSize();
        int defaultSize = paginationProperties.getDefaultPageSize();
        
        // Ensure page size is within acceptable bounds
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : defaultSize;
        pageSize = Math.min(pageSize, maxSize);
        
        // Ensure page number is non-negative
        int pageNumber = Math.max(0, pageable.getPageNumber());
        
        // Sanitize sort
        Sort sanitizedSort = sanitizeSort(pageable.getSort());
        
        return PageRequest.of(pageNumber, pageSize, sanitizedSort);
    }
    
    /**
     * Sanitizes sort by filtering allowed properties and limiting sort fields.
     */
    private Sort sanitizeSort(Sort sort) {
        if (sort.isUnsorted()) {
            // Default sort by createdAt descending
            return Sort.by("createdAt").descending();
        }
        
        List<Sort.Order> validOrders = sort.stream()
            .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
            .sorted(Comparator.comparing(Sort.Order::getProperty)) // Consistent order
            .limit(MAX_SORT_PROPERTIES)
            .toList();
        
        if (validOrders.isEmpty()) {
            log.warn("All sort properties were invalid, using default sort");
            return Sort.by("createdAt").descending();
        }
        
        return Sort.by(validOrders);
    }
}
