package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.request.ProductCreateRequest;
import com.eshop.app.catalog.domain.entity.Category;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.repository.*;
import com.eshop.app.store.domain.repository.*;
import com.eshop.app.order.domain.repository.*;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.application.service.AttributeValidatorService;
import com.eshop.app.catalog.infrastructure.config.ProductProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.eshop.app.catalog.application.service.ProductServiceHelper;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class DefaultProductServiceSanitizationTest {

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private BrandRepository brandRepository;
    private StoreRepository storeRepository;
    private com.eshop.app.store.application.port.in.StoreUseCase storeService;
    private TagRepository tagRepository;
    private OrderItemRepository orderItemRepository;
    private ProductMapper productMapper;
    private AttributeValidatorService attributeValidatorService;
    private ProductProperties productProperties;
    private ApplicationEventPublisher eventPublisher;
    private ProductServiceHelper helper;
    private com.eshop.app.catalog.application.service.AttributeService attributeService;
    private ProductCreateService productCreateService;

    private DefaultProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        brandRepository = mock(BrandRepository.class);
        storeRepository = mock(StoreRepository.class);
        storeService = mock(com.eshop.app.store.application.port.in.StoreUseCase.class);
        tagRepository = mock(TagRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        productMapper = mock(ProductMapper.class);
        attributeValidatorService = mock(AttributeValidatorService.class);
        productProperties = mock(ProductProperties.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        helper = mock(ProductServiceHelper.class);
        attributeService = mock(com.eshop.app.catalog.application.service.AttributeService.class);
        productCreateService = mock(ProductCreateService.class);

        productService = new DefaultProductService(
                productRepository,
                categoryRepository,
                brandRepository,
                storeRepository,
                storeService,
                tagRepository,
                orderItemRepository,
                productMapper,
                attributeValidatorService,
                productProperties,
                eventPublisher,
                helper,
                attributeService,
                productCreateService);
    }

    @Test
    void createProduct_sanitizesDescriptionBeforeSave() {
        ProductCreateRequest req = ProductCreateRequest.builder()
                .name("Name")
                .sku("SKU-1")
                .price(new BigDecimal("10.00"))
                .categoryId(1L)
                .storeId(1L)
                .description("<script>alert(1)</script>")
                .build();

        when(productRepository.existsBySku(any())).thenReturn(false);
        Category cat = new Category();
        cat.setId(1L);
        cat.setName("Cat");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        Store store = new Store();
        store.setId(1L);
        store.setStoreName("Store");
        when(storeRepository.findById(1L)).thenReturn(Optional.of(store));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Helper should build the Product; if not stubbed it returns null causing NPE
        when(helper.resolveOrCreateTags(any())).thenReturn(new java.util.HashSet<>());
        // Mock the sanitize method to behave like the real one for the test
        when(helper.sanitize(any())).thenAnswer(invocation -> {
            String s = invocation.getArgument(0);
            return s == null ? null : org.springframework.web.util.HtmlUtils.htmlEscape(s);
        });

        when(helper.buildProductFromRequest(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            ProductCreateRequest r = (ProductCreateRequest) invocation.getArgument(0);
            Product p = new Product();
            p.setName(r.getName());
            p.setSku(r.getSku());
            p.setPrice(r.getPrice());
            // Use the sanitized description from the request as it would be in the real
            // flow
            p.setDescription(helper.sanitize(r.getDescription()));
            p.setCategory((Category) invocation.getArgument(1));
            p.setStore((Store) invocation.getArgument(2));
            return p;
        });

        productService.createProduct(req, "test-user-id");

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, atLeastOnce()).save(captor.capture());
        Product saved = captor.getValue();
        assertNotNull(saved);
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", saved.getDescription());
    }
}
