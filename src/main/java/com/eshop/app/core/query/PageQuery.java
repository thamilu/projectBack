package com.eshop.app.core.query;

import org.springframework.data.domain.Sort;

/**
 * [HARDEN] Reusable page query request — standardized across all modules.
 *
 * Prevents duplicate pagination+filter boilerplate in every module.
 * All list-based query use cases should accept PageQuery or extend it.
 *
 * Governance:
 * - pageSize must never exceed ApiConstants.Pagination.MAX_SIZE
 * - Enforced by PageableValidationAspect at the controller layer
 *
 * @param pageNumber 0-indexed page number (default: 0)
 * @param pageSize   items per page (default: 20, max: 100)
 * @param sortBy     field to sort by (must be whitelisted)
 * @param sortDir    sort direction: ASC or DESC
 */
public record PageQuery(
        int pageNumber,
        int pageSize,
        String sortBy,
        Sort.Direction sortDir) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final Sort.Direction DEFAULT_SORT_DIR = Sort.Direction.DESC;

    /** Factory method with all defaults applied. */
    public static PageQuery defaults() {
        return new PageQuery(DEFAULT_PAGE, DEFAULT_SIZE, DEFAULT_SORT_BY, DEFAULT_SORT_DIR);
    }

    /** Factory method for explicit page control. */
    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, DEFAULT_SORT_BY, DEFAULT_SORT_DIR);
    }

    /** Factory method with full control. */
    public static PageQuery of(int page, int size, String sortBy, Sort.Direction direction) {
        return new PageQuery(page, size, sortBy, direction);
    }
}
