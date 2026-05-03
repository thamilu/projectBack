package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.function.Function;

/**
 * Generic paginated response wrapper for REST APIs.
 * Compatible with React Query pagination patterns.
 *
 * @param <T> The type of data in the page
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    /**
     * List of items in the current page
     */
    private List<T> data;

    /**
     * Pagination metadata
     */
    private PageMetadata pagination;

    /**
     * Metadata about the pagination state
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PageMetadata {
        /** Current page number (0-indexed) */
        private int page;
        /** Number of items per page */
        private int size;
        /** Total number of elements across all pages */
        private long totalElements;
        /** Total number of pages */
        private int totalPages;
        /** Whether there is a next page */
        private boolean hasNext;
        /** Whether there is a previous page */
        private boolean hasPrevious;
        /** Whether this is the first page */
        private boolean isFirst;
        /** Whether this is the last page */
        private boolean isLast;
        /** Whether the page is empty */
        private boolean isEmpty;
        /** Sort criteria (e.g., "createdAt,desc") */
        private String sort;
        /** Number of elements in current page */
        private int numberOfElements;
    }

    /**
     * Convert Spring Data Page to PageResponse
     * Time Complexity: O(1) - Just wrapping the existing list
     * Space Complexity: O(1) - No new data structures created
     *
     * @param page Spring Data Page object
     * @param <T> Type of page content
     * @return PageResponse wrapper
     * @throws IllegalArgumentException if page is null
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        if (page == null) {
            throw new IllegalArgumentException("Page cannot be null");
        }
        PageMetadata metadata = PageMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .isEmpty(page.isEmpty())
            .numberOfElements(page.getNumberOfElements())
            .sort(formatSort(page.getSort()))
            .build();
        return PageResponse.<T>builder()
            .data(page.getContent() != null
                ? Collections.unmodifiableList(page.getContent())
                : Collections.emptyList())
            .pagination(metadata)
            .build();
    }

    /**
     * Convert Spring Data Page to PageResponse with mapping function for content
     *
     * @param page Spring Data Page object
     * @param mapper Function to map page elements
     * @param <S> Source element type
     * @param <T> Target element type
     * @return PageResponse wrapper with mapped content
     */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<? super S, ? extends T> mapper) {
        if (page == null) {
            throw new IllegalArgumentException("Page cannot be null");
        }
        PageMetadata metadata = PageMetadata.builder()
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .isFirst(page.isFirst())
            .isLast(page.isLast())
            .isEmpty(page.isEmpty())
            .numberOfElements(page.getNumberOfElements())
            .sort(formatSort(page.getSort()))
            .build();

        java.util.List<T> mapped = page.getContent() != null
            ? java.util.Collections.unmodifiableList(page.getContent().stream().map(mapper).collect(Collectors.toList()))
            : java.util.Collections.emptyList();

        return PageResponse.<T>builder()
            .data(mapped)
            .pagination(metadata)
            .build();
    }

    /**
     * Create empty page response
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     *
     * @param <T> Type of page content
     * @return Empty PageResponse
     */
    public static <T> PageResponse<T> empty() {
        PageMetadata metadata = PageMetadata.builder()
            .page(0)
            .size(0)
            .totalElements(0)
            .totalPages(0)
            .hasNext(false)
            .hasPrevious(false)
            .isFirst(true)
            .isLast(true)
            .isEmpty(true)
            .numberOfElements(0)
            .build();
        return PageResponse.<T>builder()
            .data(Collections.emptyList())
            .pagination(metadata)
            .build();
    }

    /**
     * Format Sort object to string representation
     * Time Complexity: O(n) where n is number of sort orders
     * Space Complexity: O(n)
     *
     * @param sort Spring Data Sort object
     * @return Formatted sort string (e.g., "createdAt,desc;name,asc")
     */
    private static String formatSort(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return null;
        }
        return sort.stream()
            .map(order -> order.getProperty() + "," + order.getDirection().name().toLowerCase())
            .collect(Collectors.joining(";"));
    }

    /**
     * Check if this is the first page
     * Time Complexity: O(1)
     *
     * @return true if first page
     */
    public boolean isFirst() {
        return pagination != null && pagination.isFirst;
    }

    /**
     * Check if this is the last page
     * Time Complexity: O(1)
     *
     * @return true if last page
     */
    public boolean isLast() {
        return pagination != null && pagination.isLast;
    }

    /**
     * Check if page is empty
     * Time Complexity: O(1)
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

    /**
     * Get total number of elements
     * Time Complexity: O(1)
     *
     * @return total elements
     */
    public long getTotalElements() {
        return pagination != null ? pagination.totalElements : 0;
    }

    /**
     * Compatibility getter used by templates and SpEL which expect a `content` property.
     * Delegates to `data` field to preserve existing API shape.
     */
    public List<T> getContent() {
        return this.data;
    }

    /**
     * Compatibility setter for `content`.
     */
    public void setContent(List<T> content) {
        this.data = content;
    }

    public int getPage() {
        return pagination != null ? pagination.page : 0;
    }

    public int getSize() {
        return pagination != null ? pagination.size : 0;
    }

    public int getTotalPages() {
        return pagination != null ? pagination.totalPages : 0;
    }
}