package com.eshop.app.core.specification;

import org.springframework.data.jpa.domain.Specification;

/**
 * [HARDEN] Generic base contract for JPA Specification builders.
 * All module-specific specification builders should implement this interface.
 *
 * Provides:
 * - Type-safe specification construction
 * - Standardized build method signature
 * - Foundation for reusable query composition
 *
 * @param <E> the JPA entity type
 * @param <C> the search criteria DTO type
 */
public interface BaseSpecificationBuilder<E, C> {

    /**
     * Build a JPA Specification from the given search criteria.
     * Implementations must handle null criteria gracefully.
     *
     * @param criteria the search criteria, may be null
     * @return a composed Specification (never null; use conjunction() for empty criteria)
     */
    Specification<E> build(C criteria);
}
