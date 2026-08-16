package com.eshop.app.core.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * [HARDEN] Unified pagination utility.
 * Centralizes Pageable creation to ensure consistent sorting and page numbering.
 */
public final class PaginationUtils {
    
    private PaginationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Pageable createPageable(int page, int size, String sortBy, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }

    public static Pageable createPageableWithFieldDesc(int page, int size, String field) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, field));
    }
    
    public static Pageable createPageableWithFieldAsc(int page, int size, String field) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, field));
    }

    public static Pageable createPageableWithCreatedAtDesc(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}

