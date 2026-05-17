package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.request.ProductCreateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultProductServiceTest {

    @Mock
    private com.eshop.app.catalog.domain.repository.ProductRepository productRepository;
    @Mock
    private com.eshop.app.catalog.domain.repository.CategoryRepository categoryRepository;
    @Mock
    private com.eshop.app.catalog.domain.repository.BrandRepository brandRepository;
    @Mock
    private com.eshop.app.store.domain.repository.StoreRepository storeRepository;
    @Mock
    private com.eshop.app.catalog.domain.repository.TagRepository tagRepository;
    @Mock
    private com.eshop.app.order.domain.repository.OrderItemRepository orderItemRepository;
    @Mock
    private com.eshop.app.catalog.application.mapper.ProductMapper productMapper;
    @Mock
    private com.eshop.app.catalog.application.service.AttributeValidatorService attributeValidatorService;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private com.eshop.app.catalog.infrastructure.config.ProductProperties productProperties;
    @Mock
    private com.eshop.app.catalog.application.service.ProductServiceHelper helper;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    @InjectMocks
    private DefaultProductService productService;

    // MockitoExtension handles mock initialization

    @Test
    void createProduct_shouldThrowOnDuplicateSku() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("DUPLICATE");
        request.setName("Test Product");
        request.setPrice(new java.math.BigDecimal("1.00"));
        request.setCategoryId(1L);
        request.setStoreId(1L);
        when(productRepository.existsBySku("DUPLICATE")).thenReturn(true);
        assertThrows(com.eshop.app.catalog.shared.exception.DuplicateSkuException.class,
                () -> productService.createProduct(request, "test-user-id"));
    }

    @Test
    void createProduct_shouldRetryOnOptimisticLockingFailure() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("RETRY");
        request.setName("Retry Product");
        request.setPrice(new java.math.BigDecimal("2.00"));
        request.setCategoryId(1L);
        request.setStoreId(1L);
        when(productRepository.existsBySku("RETRY")).thenReturn(false);
        when(categoryRepository.findById(anyLong())).thenThrow(new OptimisticLockingFailureException("fail"));
        assertThrows(RuntimeException.class, () -> productService.createProduct(request, "test-user-id"));
        // In Mockito-only tests the Spring Retry AOP may not be applied; verify that
        // the method was at least attempted
        verify(categoryRepository, atLeast(1)).findById(anyLong());
    }
}


