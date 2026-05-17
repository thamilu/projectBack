package com.eshop.app.core.web.aspect;

import com.eshop.app.core.infrastructure.config.web.PaginationProperties;
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
 * [HARDEN] AOP aspect for automatic Pageable sanitization.
 * Intercepts all controller methods to enforce pagination bounds and safe sort fields.
 * Lives in core/web/aspect as a reusable cross-cutting concern.
 *
 * Governs:
 * - Max page size from PaginationProperties
 * - Sort property whitelist to prevent arbitrary column injection
 * - Default sort fallback
 */
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
@Slf4j
public class PageableValidationAspect {

    private final PaginationProperties paginationProperties;

    /** Whitelist of safe sort column names. Prevents arbitrary column injection. */
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
        "id", "name", "price", "createdAt", "updatedAt",
        "stockQuantity", "sku", "featured", "active"
    );

    private static final int MAX_SORT_PROPERTIES = 3;

    @Around("execution(* com.eshop.app..api.controller..*(.., org.springframework.data.domain.Pageable))")
    public Object validatePageable(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Pageable p) {
                Pageable sanitized = sanitizePageable(p);
                args[i] = sanitized;
                log.debug("Sanitized pageable: page={}, size={}, sort={}",
                    sanitized.getPageNumber(), sanitized.getPageSize(), sanitized.getSort());
            }
        }
        return joinPoint.proceed(args);
    }

    private Pageable sanitizePageable(Pageable pageable) {
        int maxSize = paginationProperties.getMaxPageSize();
        int defaultSize = paginationProperties.getDefaultPageSize();

        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : defaultSize;
        pageSize = Math.min(pageSize, maxSize);
        int pageNumber = Math.max(0, pageable.getPageNumber());
        Sort sanitizedSort = sanitizeSort(pageable.getSort());

        return PageRequest.of(pageNumber, pageSize, sanitizedSort);
    }

    private Sort sanitizeSort(Sort sort) {
        if (sort.isUnsorted()) {
            return Sort.by("createdAt").descending();
        }

        List<Sort.Order> validOrders = sort.stream()
            .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
            .sorted(Comparator.comparing(Sort.Order::getProperty))
            .limit(MAX_SORT_PROPERTIES)
            .toList();

        if (validOrders.isEmpty()) {
            log.warn("All sort properties were invalid, using default sort");
            return Sort.by("createdAt").descending();
        }

        return Sort.by(validOrders);
    }
}
