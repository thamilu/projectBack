package com.eshop.app.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility class for consistent pagination handling across controllers
 * 
 * Provides reusable methods for creating Pageable objects with standard sorting
 * This follows DRY principle by centralizing pagination logic
 * 
 * @author E-Shop Team
 * @version 1.0
 */
public final class PaginationUtils {
    
    private PaginationUtils() {
        // Utility class - prevent instantiation
    }
    
    // Default pagination constants
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 1000;
    
    /**
     * Creates a Pageable object with default sort by createdAt descending
     * Useful for list endpoints returning paginated results
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @return Pageable configured with createdAt descending sort
     */
    public static Pageable createPageableWithCreatedAtDesc(int page, int size) {
        return PageRequest.of(page, size, Sort.by("createdAt").descending());
    }
    
    /**
     * Creates a Pageable object with custom sort field in descending order
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @param sortField Field name to sort by
     * @return Pageable configured with specified sort field descending
     */
    public static Pageable createPageableWithFieldDesc(int page, int size, String sortField) {
        return PageRequest.of(page, size, Sort.by(sortField).descending());
    }
    
    /**
     * Creates a Pageable object with custom sort field and direction
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @param sortField Field name to sort by
     * @param direction Sort direction (ASC or DESC)
     * @return Pageable configured with specified sort field and direction
     */
    public static Pageable createPageable(int page, int size, String sortField, Sort.Direction direction) {
        return PageRequest.of(page, size, Sort.by(direction, sortField));
    }
    
    /**
     * Creates a Pageable object with multiple sort fields
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @param orders Variable number of Sort.Order objects
     * @return Pageable configured with specified sort orders
     */
    public static Pageable createPageable(int page, int size, Sort.Order... orders) {
        return PageRequest.of(page, size, Sort.by(orders));
    }
    
    /**
     * Creates a Pageable object without sorting (useful for simple list endpoints)
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @return Pageable without any sort specification
     */
    public static Pageable createPageableWithoutSort(int page, int size) {
        return PageRequest.of(page, size);
    }
    
    /**
     * Validates and constrains page size to prevent abuse
     * 
     * @param requestedSize User-requested page size
     * @param maxAllowed Maximum allowed page size
     * @return Constrained page size
     */
    public static int constrainPageSize(int requestedSize, int maxAllowed) {
        return Math.min(requestedSize, maxAllowed);
    }
    
    /**
     * Validates page and size parameters
     * 
     * @param page Zero-indexed page number
     * @param size Page size
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static void validatePaginationParams(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than 0");
        }
        if (size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }
    }
}
