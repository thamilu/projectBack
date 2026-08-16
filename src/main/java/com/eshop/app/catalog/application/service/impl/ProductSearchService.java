package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.response.ProductResponse;
import com.eshop.app.catalog.api.response.ProductSearchCriteria;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.core.api.response.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> searchProducts(
            ProductSearchCriteria criteria, Pageable pageable) {
        Specification<Product> spec = buildProductSpecification(criteria);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    @Cacheable(value = "featuredProducts", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> getFeaturedProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByFeatured(true, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    @Cacheable(
            value = "productList",
            key =
                    "'tags:' + #tags?.toString() + ':' + #pageable.pageNumber + ':' +"
                            + " #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> getProductsByTags(Set<String> tags, Pageable pageable) {
        Page<Product> page = productRepository.findByTagNames(tags, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> fullTextSearch(String query, Pageable pageable) {
        Page<Product> page = productRepository.fullTextSearch(query, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    private Specification<Product> buildProductSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
                String k = "%" + criteria.getKeyword().toLowerCase() + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("name")), k),
                                cb.like(cb.lower(root.get("description")), k),
                                cb.like(cb.lower(root.get("sku")), k)));
            }
            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
