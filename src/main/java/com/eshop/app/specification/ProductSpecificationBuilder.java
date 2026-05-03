package com.eshop.app.specification;

import com.eshop.app.dto.request.ProductSearchCriteria;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder for Product JPA Specifications.
 * Provides safe, complete filtering with SQL injection protection.
 * 
 * @since 2.0
 */
@Component
@RequiredArgsConstructor
public class ProductSpecificationBuilder {

    private static final char ESCAPE_CHAR = '\\';

    /**
     * Build complete specification from search criteria.
     * All filters are properly escaped and null-safe.
     * 
     * @param criteria the search criteria
     * @return JPA Specification for query building
     */
    public Specification<Product> build(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // Active filter (default to active products only)
            if (criteria.getActive() == null || Boolean.TRUE.equals(criteria.getActive())) {
                predicates.add(cb.isTrue(root.get("active")));
            } else if (Boolean.FALSE.equals(criteria.getActive())) {
                predicates.add(cb.isFalse(root.get("active")));
            }

            // Keyword search with SQL injection protection
            if (StringUtils.hasText(criteria.getKeyword())) {
                String pattern = createSafeLikePattern(criteria.getKeyword());
                Predicate keywordPredicate = cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("description")), pattern, ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("sku")), pattern, ESCAPE_CHAR));
                predicates.add(keywordPredicate);
            }

            // Category filter
            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
            }

            // Multiple categories
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(criteria.getCategoryIds()));
            }

            // Brand filter
            if (criteria.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), criteria.getBrandId()));
            }

            // Multiple brands
            if (criteria.getBrandIds() != null && !criteria.getBrandIds().isEmpty()) {
                predicates.add(root.get("brand").get("id").in(criteria.getBrandIds()));
            }

            // Store filter
            if (criteria.getStoreId() != null) {
                predicates.add(cb.equal(root.get("store").get("id"), criteria.getStoreId()));
            }

            // Price range
            if (criteria.getMinPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), criteria.getMinPrice()));
            }
            if (criteria.getMaxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), criteria.getMaxPrice()));
            }

            // Discount filter
            if (Boolean.TRUE.equals(criteria.getHasDiscount())) {
                predicates.add(cb.isNotNull(root.get("discountPrice")));
                predicates.add(cb.greaterThan(root.get("discountPrice"), BigDecimal.ZERO));
                predicates.add(cb.lessThan(root.get("discountPrice"), root.get("price")));
            }

            // Stock availability
            if (Boolean.TRUE.equals(criteria.getInStock())) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            } else if (Boolean.FALSE.equals(criteria.getInStock())) {
                predicates.add(cb.equal(root.get("stockQuantity"), 0));
            }

            // Featured filter
            if (criteria.getFeatured() != null) {
                predicates.add(cb.equal(root.get("featured"), criteria.getFeatured()));
            }

            // Tags filter (requires join)
            if (criteria.getTags() != null && !criteria.getTags().isEmpty()) {
                Join<Product, Tag> tagJoin = root.join("tags", JoinType.INNER);
                predicates.add(tagJoin.get("name").in(criteria.getTags()));
            }

            // Date range filters
            if (criteria.getCreatedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), criteria.getCreatedAfter()));
            }
            if (criteria.getCreatedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), criteria.getCreatedBefore()));
            }

            // Seller filter (for admin dashboards)
            if (criteria.getSellerId() != null) {
                predicates.add(cb.equal(root.get("store").get("seller").get("id"), criteria.getSellerId()));
            }

            // Ensure distinct results when using joins
            query.distinct(true);

            // Apply default ordering if not specified
            if (query != null && query.getResultType() != null &&
                    !query.getResultType().equals(Long.class) &&
                    (query.getOrderList() == null || query.getOrderList().isEmpty())) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Create safe LIKE pattern with escaped special characters.
     * Prevents SQL injection through LIKE wildcards.
     * 
     * @param keyword the user input keyword
     * @return escaped pattern safe for LIKE queries
     */
    private String createSafeLikePattern(String keyword) {
        if (keyword == null) {
            return "%";
        }

        String escaped = keyword.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        return "%" + escaped + "%";
    }
}
