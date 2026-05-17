package com.eshop.app.catalog.application.specification;

import com.eshop.app.catalog.domain.entity.Tag;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

import com.eshop.app.catalog.api.response.ProductSearchCriteria;
import com.eshop.app.catalog.domain.entity.Product;

import com.eshop.app.core.specification.BaseSpecificationBuilder;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * [HARDEN] Product JPA Specification Builder.
 * Provides safe, SQL-injection-protected filtering for product queries.
 *
 * Moved from root specification/ to catalog/application/specification/ — proper
 * bounded context ownership.
 * Implements BaseSpecificationBuilder for standardized contract.
 *
 * Features:
 * - Keyword search across name, description, SKU (escaped LIKE)
 * - Category, Brand, Store, Tag filters
 * - Price range, stock availability, featured filters
 * - Date range filters for admin dashboards
 * - Distinct results enforcement for join queries
 */
@Component
@RequiredArgsConstructor
public class ProductSpecificationBuilder implements BaseSpecificationBuilder<Product, ProductSearchCriteria> {

    private static final char ESCAPE_CHAR = '\\';

    @Override
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

            // Keyword search with SQL injection protection via ESCAPE_CHAR
            if (StringUtils.hasText(criteria.getKeyword())) {
                String pattern = createSafeLikePattern(criteria.getKeyword());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("description")), pattern, ESCAPE_CHAR),
                        cb.like(cb.lower(root.get("sku")), pattern, ESCAPE_CHAR)));
            }

            // Category filters
            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
            }
            if (criteria.getCategoryIds() != null && !criteria.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(criteria.getCategoryIds()));
            }

            // Brand filters
            if (criteria.getBrandId() != null) {
                predicates.add(cb.equal(root.get("brand").get("id"), criteria.getBrandId()));
            }
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

            // Tags filter (INNER JOIN to ensure product has all given tags)
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

            // Seller filter (admin dashboards)
            if (criteria.getSellerId() != null) {
                predicates.add(cb.equal(root.get("store").get("seller").get("id"), criteria.getSellerId()));
            }

            // Distinct required for joins
            query.distinct(true);

            // Default ordering if none specified
            if (query.getResultType() != null
                    && !query.getResultType().equals(Long.class)
                    && (query.getOrderList() == null || query.getOrderList().isEmpty())) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a safe LIKE pattern with ESCAPE_CHAR-escaped SQL wildcards.
     * Prevents SQL injection through LIKE clause wildcards.
     */
    private String createSafeLikePattern(String keyword) {
        if (keyword == null)
            return "%";
        String escaped = keyword.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
