package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.response.ProductListResponse;
import com.eshop.app.catalog.api.response.ProductResponse;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private static final int MAX_BATCH_SIZE = 100;

    @Cacheable(value = "products", key = "#id", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductById(Long id) {
        return productRepository.findById(id).map(productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductBySku(String sku) {
        return productRepository.findBySku(sku).map(productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductByFriendlyUrl(String friendlyUrl) {
        return productRepository
                .findByFriendlyUrl(friendlyUrl)
                .map(productMapper::toProductResponse);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Map<Long, ProductResponse> getProductsByIds(Set<Long> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        if (ids.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Batch size " + ids.size() + " exceeds maximum " + MAX_BATCH_SIZE);
        }

        List<Product> products = productRepository.findAllById(ids);
        return products.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(p -> p.getId(), p -> productMapper.toProductResponse(p)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductById(Long id) {
        return findProductById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductBySku(String sku) {
        return findProductBySku(sku)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Product not found with SKU: " + sku));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductByFriendlyUrl(String friendlyUrl) {
        return findProductByFriendlyUrl(friendlyUrl)
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Product not found with URL: " + friendlyUrl));
    }

    @Cacheable(
            value = "productList",
            key =
                    "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' +"
                            + " #pageable.sort.toString()",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getAllProducts(Pageable pageable) {
        int maxPageSize = 50;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
        }
        var page = productRepository.findAllSummaries(pageable);
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Cacheable(
            value = "productList",
            key =
                    "'category:' + #categoryId + ':' + #pageable.pageNumber + ':' +"
                            + " #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByCategory(
            Long categoryId, Pageable pageable) {
        var page = productRepository.findSummariesByCategory(categoryId, pageable);
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Cacheable(
            value = "productList",
            key = "'brand:' + #brandId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByBrand(Long brandId, Pageable pageable) {
        var page = productRepository.findSummariesByBrand(brandId, pageable);
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Cacheable(
            value = "productList",
            key = "'store:' + #storeId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByStore(Long storeId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByStoreId(storeId, pageable);
        Page<ProductListResponse> responsePage =
                productPage.map(productMapper::toProductListResponseFromEntity);
        return PageResponse.of(responsePage);
    }

    @Cacheable(
            value = "productSearch",
            key = "#keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
            sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.searchProducts(keyword, pageable);
        Page<ProductListResponse> responsePage =
                productPage.map(productMapper::toProductListResponseFromEntity);
        return PageResponse.of(responsePage);
    }
}
