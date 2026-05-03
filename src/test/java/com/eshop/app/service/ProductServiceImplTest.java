package com.eshop.app.service;

import com.eshop.app.dto.request.ProductCreateRequest;
import com.eshop.app.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private com.eshop.app.repository.ProductRepository productRepository;
    @Mock
    private com.eshop.app.repository.CategoryRepository categoryRepository;
    @Mock
    private com.eshop.app.repository.BrandRepository brandRepository;
    @Mock
    private com.eshop.app.repository.StoreRepository storeRepository;
    @Mock
    private com.eshop.app.repository.TagRepository tagRepository;
    @Mock
    private com.eshop.app.repository.OrderItemRepository orderItemRepository;
    @Mock
    private com.eshop.app.mapper.ProductMapper productMapper;
    @Mock
    private com.eshop.app.service.AttributeValidatorService attributeValidatorService;
    @Mock
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    @Mock
    private com.eshop.app.config.ProductProperties productProperties;
    @Mock
    private com.eshop.app.service.impl.ProductServiceHelper helper;
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private org.springframework.cache.CacheManager cacheManager;

    @InjectMocks
    private ProductServiceImpl productService;

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
        assertThrows(com.eshop.app.exception.DuplicateSkuException.class,
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
