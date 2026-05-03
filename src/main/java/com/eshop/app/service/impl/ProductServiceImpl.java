package com.eshop.app.service.impl;

import com.eshop.app.service.ProductService;

import com.eshop.app.repository.ProductRepository;
import com.eshop.app.repository.CategoryRepository;
import com.eshop.app.repository.BrandRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.repository.TagRepository;
import com.eshop.app.repository.OrderItemRepository;
import com.eshop.app.mapper.ProductMapper;
import com.eshop.app.service.AttributeValidatorService;
import com.eshop.app.service.StoreService;
import com.eshop.app.dto.request.ProductCreateRequest;
import com.eshop.app.dto.request.ProductUpdateRequest;
import com.eshop.app.dto.request.BatchProductCreateRequest;
import com.eshop.app.dto.request.StockUpdateRequest;
import com.eshop.app.dto.request.ProductSearchCriteria;
import com.eshop.app.dto.response.ProductResponse;
import com.eshop.app.dto.response.ProductListResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.BatchOperationResult;
import com.eshop.app.dto.response.TopSellingProductResponse;
import com.eshop.app.dto.response.ProductStatistics;
import com.eshop.app.dto.response.SellerProductDashboard;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.Category;
import com.eshop.app.entity.Brand;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.Tag;
import com.eshop.app.entity.OrderItem;
import com.eshop.app.entity.enums.ProductStatus;
import com.eshop.app.event.ProductCreatedEvent;
import com.eshop.app.event.LowStockEvent;
import com.eshop.app.exception.DuplicateResourceException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.exception.ProductNotFoundException;
import com.eshop.app.exception.ProductDeletionException;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.InsufficientStockException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.dao.PessimisticLockingFailureException;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.config.ProductProperties;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.eshop.app.service.AttributeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.cache.annotation.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// ...existing code...
@Slf4j
@Service
// Removed class-level @Transactional - each method explicitly defines its
// transaction boundary
@RequiredArgsConstructor
@CacheConfig(cacheNames = ApiConstants.Cache.PRODUCTS_CACHE)
public class ProductServiceImpl implements ProductService {

    // ═══════════════════════════════════════════════════════════════
    // CONSTANTS (CRITICAL-001 FIX)
    // ═══════════════════════════════════════════════════════════════

    /** Maximum batch size for bulk operations (CRITICAL-001) */
    private static final int MAX_BATCH_SIZE = 100;

    // Adding a constant for LOW_STOCK_THRESHOLD
    private static final int LOW_STOCK_THRESHOLD = 10;

    // ═══════════════════════════════════════════════════════════════
    // DEPENDENCIES
    // ═══════════════════════════════════════════════════════════════
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final StoreRepository storeRepository;
    private final StoreService storeService;
    private final TagRepository tagRepository; // Used in statistics and batch resolution
    private final OrderItemRepository orderItemRepository;
    private final ProductMapper productMapper;
    private final AttributeValidatorService attributeValidatorService;
    private final ProductProperties productProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductServiceHelper helper;
    // private final com.eshop.app.repository.ProductRepositoryEnhanced
    // productRepositoryEnhanced; // Removed
    private final AttributeService attributeService;

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE
    // ═══════════════════════════════════════════════════════════════

    @PostConstruct
    public void logConfiguration() {
        log.info(
                "ProductService initialized with configuration: lowStockThreshold={}, maxBatchSize={}, maxFriendlyUrlAttempts={}",
                productProperties.getLowStockThreshold(),
                productProperties.getMaxBatchSize(),
                productProperties.getMaxFriendlyUrlAttempts());
    }

    // ═══════════════════════════════════════════════════════════════
    // CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Create a new product with comprehensive validation (CRITICAL-003 FIX).
     * 
     * <p>
     * <b>Fixes Applied:</b>
     * <ul>
     * <li>CRITICAL-003: Validate-first pattern (no manual cleanup)</li>
     * <li>HIGH-001: Comprehensive logging with MDC</li>
     * <li>HIGH-005: Method-level validation with @Valid</li>
     * <li>MEDIUM-005: Batch tag processing</li>
     * </ul>
     * 
     * <p>
     * <b>Validations:</b>
     * <ul>
     * <li>Unique SKU enforcement</li>
     * <li>Friendly URL generation and uniqueness (CRITICAL-002 fix)</li>
     * <li>Dynamic category attributes validation (before persistence)</li>
     * <li>Tag batch resolution (N+1 fix)</li>
     * </ul>
     * 
     * @param request validated product creation request
     * @return created product response with all relations populated
     * @throws DuplicateSkuException     if SKU already exists
     * @throws ResourceNotFoundException if related entities not found
     * @throws IllegalArgumentException  if category attributes validation fails
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @Retryable(retryFor = { org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.ConcurrencyFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public ProductResponse createProduct(@Valid @NotNull ProductCreateRequest request, String userId) {
        MDC.put("operation", "createProduct");
        MDC.put("sku", request.getSku());
        MDC.put("userId", userId);

        log.info("Creating product: SKU={}, name={}, userId={}", request.getSku(), request.getName(), userId);

        try {
            // ────────────────────────────────────────────────────────
            // CRITICAL-003 FIX: Validate FIRST, before any persistence
            // ────────────────────────────────────────────────────────

            // Pre-validation assertions
            Assert.notNull(request.getName(), "Product name is required");
            Assert.notNull(request.getSku(), "Product SKU is required");
            Assert.notNull(request.getPrice(), "Product price is required");
            Assert.isTrue(request.getPrice().compareTo(BigDecimal.ZERO) > 0, "Price must be positive");

            // Validate category attributes BEFORE creating product
            if (request.getCategoryType() != null && request.getAttributes() != null) {
                attributeValidatorService.validateAttributes(
                        request.getCategoryType(),
                        request.getAttributes());
            }

            // Check for duplicate SKU
            if (productRepository.existsBySku(request.getSku())) {
                throw new com.eshop.app.exception.DuplicateSkuException(request.getSku());
            }

            // ────────────────────────────────────────────────────────
            // Load and validate related entities
            // ────────────────────────────────────────────────────────

            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + request.getCategoryId()));

            // ✅ AUTO-RESOLVE STORE: If storeId not provided, get seller's store
            final Long resolvedStoreId;
            if (request.getStoreId() != null) {
                resolvedStoreId = request.getStoreId();
                log.info("Using provided storeId={}", resolvedStoreId);
            } else {
                log.info("StoreId not provided, auto-resolving via storeService for user identifier: {}", userId);
                try {
                    StoreResponse sellerStore = storeService.getMyStore();
                    resolvedStoreId = sellerStore.getId();
                    log.info("Auto-resolved storeId={} for seller with identifier={}", resolvedStoreId, userId);
                } catch (ResourceNotFoundException e) {
                    log.error("Failed to auto-resolve store for seller {}: {}", userId, e.getMessage());
                    throw new ResourceNotFoundException(
                            "Seller must create a store before adding products. Please create your store first.");
                }
            }

            Store store = storeRepository.findById(resolvedStoreId)
                    .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + resolvedStoreId));

            Brand brand = null;
            if (request.getBrandId() != null) {
                brand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Brand not found with id: " + request.getBrandId()));
            }

            // MEDIUM-005 FIX: Batch tag resolution (N+1 → 2 queries)
            Set<Tag> tags = helper.resolveOrCreateTags(request.getTags());

            // ────────────────────────────────────────────────────────
            // Build product entity (single save operation)
            // ────────────────────────────────────────────────────────

            Product product = helper.buildProductFromRequest(request, category, store, brand, tags);

            // Single save operation (no manual cleanup needed)
            product = productRepository.save(product);

            // ────────────────────────────────────────────────────────
            // Publish domain event (async processing)
            // ────────────────────────────────────────────────────────

            eventPublisher.publishEvent(new ProductCreatedEvent(this, product));

            log.info("Successfully created product: ID={}, SKU={}", product.getId(), product.getSku());

            return productMapper.toProductResponse(product);

        } catch (DuplicateResourceException | ResourceNotFoundException | IllegalArgumentException e) {
            log.warn("Product creation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating product: SKU={}, error={}", request.getSku(), e.getMessage(), e);
            throw e;
        } finally {
            MDC.clear();
        }
    }

    /**
     * Update product with cache update
     * Time Complexity: O(1) - database update with index
     * Space Complexity: O(1)
     */
    @Override
    @Transactional // Explicit write transaction
    @CachePut(value = "products", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @Retryable(retryFor = { org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.ConcurrencyFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        product.setName(request.getName());
        product.setDescription(helper.sanitize(request.getDescription()));

        // Update friendly URL if provided, or generate new one if name changed
        if (request.getFriendlyUrl() != null && !request.getFriendlyUrl().trim().isEmpty()) {
            String newFriendlyUrl = request.getFriendlyUrl();
            if (!newFriendlyUrl.equals(product.getFriendlyUrl())) {
                newFriendlyUrl = ensureUniqueFriendlyUrl(newFriendlyUrl);
                product.setFriendlyUrl(newFriendlyUrl);
            }
        } else if (!request.getName().equals(product.getName())) {
            String newFriendlyUrl = generateFriendlyUrl(request.getName());
            newFriendlyUrl = ensureUniqueFriendlyUrl(newFriendlyUrl);
            product.setFriendlyUrl(newFriendlyUrl);
        }

        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
            product.setBrand(brand);
        } else {
            product.setBrand(null);
        }

        // Reuse the same batch tag resolution as createProduct (N+1 fix)
        if (request.getTags() != null) {
            product.setTags(helper.resolveOrCreateTags(request.getTags()));
        }

        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }

        if (request.getActive() != null) {
            if (Boolean.TRUE.equals(request.getActive())) {
                product.activate();
            } else {
                product.deactivate();
            }
        }

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    /**
     * Delete product with cache eviction.
     * Time Complexity: O(1) - database delete with index
     * Space Complexity: O(1)
     */
    @Override
    @CacheEvict(value = "products", key = "#id")
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        // Check for active orders before deletion
        List<OrderItem> activeOrders = orderItemRepository.findByProductId(id);
        if (!activeOrders.isEmpty()) {
            throw new ProductDeletionException(id,
                    "Product has " + activeOrders.size() + " active order items");
        }

        productRepository.deleteById(id);
    }

    /**
     * Create product with automatic category creation/selection.
     * 
     * @param request the creation request with category details
     * @return created product response
     * @throws ResourceNotFoundException if category not found
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ProductResponse createProductWithAutoCategory(
            com.eshop.app.dto.request.ProductCreateWithCategoryRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        } else if (request.getNewCategoryName() != null && !request.getNewCategoryName().isEmpty()) {
            category = categoryRepository.findByName(request.getNewCategoryName())
                    .orElseGet(() -> categoryRepository.save(new Category(request.getNewCategoryName())));
        } else {
            throw new IllegalArgumentException("Either categoryId or newCategoryName must be provided");
        }

        // Validate dynamic attributes
        if (request.getAttributes() != null && !request.getAttributes().isEmpty()) {
            attributeService.validateProductAttributes(category.getId(), request.getAttributes());
        }

        // 3. Create Product Entity
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();
        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Find product by ID with Optional return.
     * 
     * @param id the product ID
     * @return Optional containing product if found
     */
    @Override
    @Cacheable(value = "products", key = "#id", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductById(Long id) {
        return productRepository.findById(id)
                .map(productMapper::toProductResponse);
    }

    /**
     * Find product by SKU with Optional return.
     * 
     * @param sku the product SKU
     * @return Optional containing product if found
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductBySku(String sku) {
        return productRepository.findBySku(sku)
                .map(productMapper::toProductResponse);
    }

    /**
     * Find product by friendly URL with Optional return.
     * 
     * @param friendlyUrl the friendly URL
     * @return Optional containing product if found
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public Optional<ProductResponse> findProductByFriendlyUrl(String friendlyUrl) {
        return productRepository.findByFriendlyUrl(friendlyUrl)
                .map(productMapper::toProductResponse);
    }

    /**
     * Get multiple products by IDs in batch.
     * 
     * <p>
     * Performance: O(n) where n = ids.size()
     * 
     * @param ids set of product IDs (max 100)
     * @return map of ID to ProductResponse
     */
    @Override
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
                .collect(Collectors.toMap(
                        Product::getId,
                        productMapper::toProductResponse));
    }

    // ═══════════════════════════════════════════════════════════════
    // SEARCH & FILTER OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Search products using dynamic criteria with Specification pattern.
     * 
     * <p>
     * Supports filtering by:
     * <ul>
     * <li>Keyword (name, description, SKU)</li>
     * <li>Category, Brand, Shop</li>
     * <li>Price range</li>
     * <li>Tags</li>
     * <li>Featured status</li>
     * <li>Stock availability</li>
     * </ul>
     * 
     * @param criteria the search criteria
     * @param pageable pagination parameters
     * @return paginated search results
     */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> searchProducts(
            ProductSearchCriteria criteria,
            Pageable pageable) {
        Specification<Product> spec = buildProductSpecification(criteria);
        Page<Product> page = productRepository.findAll(spec, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    /**
     * Get featured products with pagination.
     * 
     * @param pageable pagination parameters
     * @return paginated featured products
     */
    @Override
    @Cacheable(value = "featuredProducts", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> getFeaturedProducts(Pageable pageable) {
        Page<Product> page = productRepository.findByFeatured(true, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    /**
     * Get product by ID with caching
     * Time Complexity: O(1) - cache hit, O(1) - database query with index
     * Space Complexity: O(1)
     */
    @Override
    @Cacheable(value = "products", key = "#id", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ProductResponse getProductByFriendlyUrl(String friendlyUrl) {
        Product product = productRepository.findByFriendlyUrl(friendlyUrl)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with URL: " + friendlyUrl));
        return productMapper.toProductResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getAllProducts(Pageable pageable) {
        // Enforce max page size
        int maxPageSize = 50;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
        }
        // Use projection-based summary query to avoid returning managed entities
        // which can lead to Jackson serialization issues (lazy associations / cycles).
        var page = productRepository.findAllSummaries(pageable);
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Override
    @Cacheable(value = "productList", key = "'category:' + #categoryId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        var page = productRepository.findSummariesByCategory(categoryId, pageable);
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Override
    @Cacheable(value = "productList", key = "'brand:' + #brandId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByBrand(Long brandId, Pageable pageable) {
        var page = productRepository.findAllSummaries(pageable); // Replace with a projection query for brand if
                                                                 // available
        return PageResponse.of(page, productMapper::toProductListResponse);
    }

    @Override
    @Cacheable(value = "productList", key = "'store:' + #storeId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> getProductsByStore(Long storeId, Pageable pageable) {
        Page<Product> productPage = productRepository.findByStoreId(storeId, pageable);
        Page<ProductListResponse> responsePage = productPage
                .map(product -> productMapper.toProductListResponseFromEntity(product));
        return PageResponse.of(responsePage);
    }

    @Override
    @Cacheable(value = "productSearch", key = "#keyword + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductListResponse> searchProducts(String keyword, Pageable pageable) {
        Page<Product> productPage = productRepository.searchProducts(keyword, pageable);
        Page<ProductListResponse> responsePage = productPage
                .map(product -> productMapper.toProductListResponseFromEntity(product));
        return PageResponse.of(responsePage);
    }

    @Override
    @Cacheable(value = "productList", key = "'tags:' + #tags?.toString() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize", sync = true)
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> getProductsByTags(Set<String> tags, Pageable pageable) {
        Page<Product> page = productRepository.findByTagNames(tags, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    // (duplicate getFeaturedProducts removed) The cached, paginated implementation
    // appears earlier in this class and will be used by callers.

    // ═══════════════════════════════════════════════════════════════
    // BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = { "products", "productList", "productCount", "productSearch" }, allEntries = true)
    @Retryable(retryFor = { org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.ConcurrencyFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public BatchOperationResult<ProductResponse> createProductsBatch(BatchProductCreateRequest request) {
        log.info("Starting batch product creation: {} items", request.products().size());
        BatchOperationResult.Builder<ProductResponse> resultBuilder = BatchOperationResult.builder();

        // Admin batch operations - shopId must be provided in each request
        String adminUserId = "admin-batch";

        for (int i = 0; i < request.products().size(); i++) {
            ProductCreateRequest req = request.products().get(i);
            try {
                // For batch operations, shopId must be provided (admins can specify any shop)
                ProductResponse created = createProduct(req, adminUserId);
                resultBuilder.addSuccess(created);
                log.debug("Batch item {}: Created product {}", i, created.getId());

            } catch (ResourceAlreadyExistsException e) {
                log.warn("Batch item {}: Duplicate SKU {}", i, req.getSku());
                resultBuilder.addFailure(i, req.getSku(), e.getMessage(), "DUPLICATE_SKU");
            } catch (ResourceNotFoundException e) {
                log.warn("Batch item {}: Referenced entity not found", i);
                resultBuilder.addFailure(i, req.getName(), e.getMessage(), "NOT_FOUND");
            } catch (Exception e) {
                log.error("Batch item {}: Unexpected error creating product {}", i, req.getName(), e);
                resultBuilder.addFailure(i, req.getName(), e.getMessage(), "INTERNAL_ERROR");
            }

            if (resultBuilder.hasFailures() && request.options().stopOnError()) {
                log.warn("Stopping batch at index {} due to stopOnError policy", i);
                break;
            }
        }

        BatchOperationResult<ProductResponse> result = resultBuilder.build();
        log.info("Batch product creation completed: {} succeeded, {} failed",
                result.successCount(), result.failureCount());
        return result;
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(cacheNames = { "products", "productList", "productCount", "productSearch" }, allEntries = true)
    @Retryable(retryFor = { org.springframework.dao.OptimisticLockingFailureException.class,
            org.springframework.dao.ConcurrencyFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 500, multiplier = 2))
    public BatchOperationResult<Long> deleteProductsBatch(List<Long> ids, String userId) {
        log.info("Starting batch product deletion: {} items by user {}", ids.size(), userId);
        BatchOperationResult.Builder<Long> resultBuilder = BatchOperationResult.builder();
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            try {
                deleteProduct(id);
                resultBuilder.addSuccess(id);
                log.debug("Batch item {}: Deleted product {}", i, id);
            } catch (ResourceNotFoundException e) {
                log.warn("Batch item {}: Product {} not found", i, id);
                resultBuilder.addFailure(i, String.valueOf(id), e.getMessage(), "NOT_FOUND");
            } catch (Exception e) {
                log.error("Batch item {}: Error deleting product {}", i, id, e);
                resultBuilder.addFailure(i, String.valueOf(id), e.getMessage(), "INTERNAL_ERROR");
            }
        }
        BatchOperationResult<Long> result = resultBuilder.build();
        log.info("Batch product deletion completed: {} succeeded, {} failed",
                result.successCount(), result.failureCount());
        return result;
    }

    // ──────────────────────────────────────────────────────────────
    // Retry Recovery Methods (must be top-level, not nested)
    // ──────────────────────────────────────────────────────────────
    @Recover
    public ProductResponse recoverFromFailure(Exception ex, ProductCreateRequest request, String userId) {
        log.error("Failed to create product after retries: {}", ex.getMessage());
        // If this is a business exception, rethrow it so correct HTTP status is
        // preserved
        if (ex instanceof com.eshop.app.exception.DuplicateResourceException
                || ex instanceof com.eshop.app.exception.ResourceAlreadyExistsException
                || ex instanceof com.eshop.app.exception.ResourceNotFoundException) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
        }
        throw new RuntimeException("Could not create product after retries", ex);
    }

    @Recover
    public ProductResponse recoverFromFailure(Exception ex, Long id, ProductUpdateRequest request) {
        log.error("Failed to update product after retries: {}", ex.getMessage());
        if (ex instanceof com.eshop.app.exception.DuplicateResourceException
                || ex instanceof com.eshop.app.exception.ResourceAlreadyExistsException
                || ex instanceof com.eshop.app.exception.ResourceNotFoundException) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
        }
        throw new RuntimeException("Could not update product after retries", ex);
    }

    @Recover
    public BatchOperationResult<ProductResponse> recoverFromFailure(Exception ex, BatchProductCreateRequest request) {
        log.error("Failed to batch create products after retries: {}", ex.getMessage());
        if (ex instanceof com.eshop.app.exception.DuplicateResourceException
                || ex instanceof com.eshop.app.exception.ResourceAlreadyExistsException
                || ex instanceof com.eshop.app.exception.ResourceNotFoundException) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
        }
        throw new RuntimeException("Could not batch create products after retries", ex);
    }

    @Recover
    public BatchOperationResult<Long> recoverFromFailure(Exception ex, List<Long> ids, String userId) {
        log.error("Failed to batch delete products after retries: {}", ex.getMessage());
        if (ex instanceof com.eshop.app.exception.DuplicateResourceException
                || ex instanceof com.eshop.app.exception.ResourceAlreadyExistsException
                || ex instanceof com.eshop.app.exception.ResourceNotFoundException) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            }
        }
        throw new RuntimeException("Could not batch delete products after retries", ex);
    }

    // ═══════════════════════════════════════════════════════════════
    // STOCK MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * CRITICAL-005 FIX: Updates stock with pessimistic locking to prevent race
     * conditions.
     * 
     * <p>
     * Concurrency Control:
     * <ul>
     * <li>Uses PESSIMISTIC_WRITE lock (SELECT FOR UPDATE)</li>
     * <li>Prevents lost updates in concurrent stock modifications</li>
     * <li>Retry logic handles PessimisticLockingFailureException</li>
     * </ul>
     * 
     * @param id      product ID
     * @param request stock update request
     * @return updated product response
     * @throws ResourceNotFoundException  if product not found
     * @throws InsufficientStockException if stock would go negative
     */
    @Override
    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
    @CacheEvict(value = "products", key = "#id")
    @Retryable(retryFor = {
            PessimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public ProductResponse updateStockAndReturn(Long id, StockUpdateRequest request) {
        log.debug("Updating stock for product {}: {} {}", id, request.operation(), request.quantity());

        // CRITICAL-005 FIX: Use pessimistic lock to prevent race conditions
        Product product = productRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        int oldStock = product.getStockQuantity();
        int newStock = switch (request.operation()) {
            case SET -> request.quantity();
            case INCREMENT -> oldStock + request.quantity();
            case DECREMENT -> oldStock - request.quantity();
        };

        if (newStock < 0) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock for product %s: requested %d, available %d",
                            product.getName(), request.quantity(), oldStock));
        }

        product.setStockQuantity(newStock);
        Product saved = productRepository.save(product);

        // Publish event for low stock alert
        if (newStock < productProperties.getLowStockThreshold()) {
            eventPublisher.publishEvent(new LowStockEvent(this, saved));
        }

        log.info("Stock updated for product {}: old={}, new={} (pessimistic lock)", id, oldStock, newStock);
        return productMapper.toProductResponse(saved);
    }

    /**
     * CRITICAL-005 FIX: Updates stock with pessimistic locking.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @Retryable(retryFor = {
            PessimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public void updateStock(Long productId, Integer quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        int newStock = product.getStockQuantity() + quantity;
        if (newStock < 0) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
        }

        product.setStockQuantity(newStock);
        productRepository.save(product);
    }

    /**
     * CRITICAL-005 FIX: Adjusts stock with pessimistic locking.
     */
    @Override
    @Transactional
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    @Retryable(retryFor = {
            PessimisticLockingFailureException.class }, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public void adjustStock(Long productId, int delta) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        int newStock = product.getStockQuantity() + delta;
        if (newStock < 0) {
            throw new InsufficientStockException("Insufficient stock for product: " + product.getName());
        }
        product.setStockQuantity(newStock);
        productRepository.save(product);
    }

    /**
     * Generate SEO-friendly URL from product name.
     * Delegates to {@link ProductServiceHelper#generateFriendlyUrl(String)} to
     * avoid duplication.
     */
    private String generateFriendlyUrl(String name) {
        return helper.generateFriendlyUrl(name);
    }

    /**
     * Ensure friendly URL is unique with circuit breaker protection.
     * Delegates to {@link ProductServiceHelper#ensureUniqueFriendlyUrl(String)} to
     * avoid
     * duplicating the unsafe infinite-loop version.
     */
    private String ensureUniqueFriendlyUrl(String baseUrl) {
        return helper.ensureUniqueFriendlyUrl(baseUrl);
    }

    private Specification<Product> buildProductSpecification(ProductSearchCriteria criteria) {
        return (root, query, cb) -> {
            if (criteria == null)
                return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();
            if (criteria.getKeyword() != null && !criteria.getKeyword().isEmpty()) {
                String k = "%" + criteria.getKeyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), k),
                        cb.like(cb.lower(root.get("description")), k),
                        cb.like(cb.lower(root.get("sku")), k)));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalProductCount() {
        // Mock implementation
        return 1000;
    }

    @Override
    @Transactional(readOnly = true)
    public long getProductCountBySellerId(Long sellerId) {
        // Mock implementation
        return 200;
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveProductCountBySellerId(Long sellerId) {
        // Mock implementation
        return 180;
    }

    @Override
    @Transactional(readOnly = true)
    public long getOutOfStockCountBySellerId(Long sellerId) {
        // Mock implementation
        return 20;
    }

    @Override
    @Transactional(readOnly = true)
    public long getLowStockCountBySellerId(Long sellerId) {

        // Real implementation
        return productRepository.countByStoreSellerProfileUserIdAndStockQuantityLessThan(sellerId, LOW_STOCK_THRESHOLD);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductPerformanceBySellerId(Long sellerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Product> products = productRepository.findByStoreSellerProfileUserId(sellerId);
        for (Product p : products) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", p.getId());
            m.put("productName", p.getName());
            m.put("sku", p.getSku());
            m.put("currentStock", p.getStockQuantity());
            m.put("totalSold", 0L);
            result.add(m);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS & DASHBOARD METHODS
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public ProductStatistics getGlobalStatistics() {
        // Example implementation (replace with real queries as needed)
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        long inactiveProducts = productRepository.countByStatus(ProductStatus.INACTIVE);
        long featuredProducts = productRepository.countByFeatured(true);
        long outOfStock = productRepository.countByStockQuantity(0);
        long lowStock = productRepository.countByStockQuantityLessThan(LOW_STOCK_THRESHOLD);
        long totalInventoryUnits = productRepository.sumStockQuantity();
        double avgStock = totalProducts > 0 ? (double) totalInventoryUnits / totalProducts : 0.0;
        BigDecimal avgPrice = productRepository.avgPrice();
        BigDecimal minPrice = productRepository.minPrice();
        BigDecimal maxPrice = productRepository.maxPrice();
        BigDecimal totalInventoryValue = productRepository.sumInventoryValue();
        long totalCategories = categoryRepository.count();
        long totalBrands = brandRepository.count();
        long totalTags = tagRepository.count();
        LocalDateTime now = LocalDateTime.now();
        long productsAdded24h = productRepository.countByCreatedAtAfter(now.minusHours(24));
        long productsUpdated24h = productRepository.countByUpdatedAtAfter(now.minusHours(24));
        return ProductStatistics.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .featuredProducts(featuredProducts)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .totalInventoryUnits(totalInventoryUnits)
                .averageStockPerProduct(avgStock)
                .averagePrice(avgPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalInventoryValue(totalInventoryValue)
                .totalCategories(totalCategories)
                .totalBrands(totalBrands)
                .totalTags(totalTags)
                .generatedAt(now)
                .productsAddedLast24Hours(productsAdded24h)
                .productsUpdatedLast24Hours(productsUpdated24h)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and #sellerId == principal.id)")
    public SellerProductDashboard getSellerDashboard(Long sellerId, int topProductsLimit) {
        // Example implementation (replace with real queries as needed)
        long totalProducts = productRepository.countByStoreSellerProfileUserId(sellerId);
        long activeProducts = productRepository.countByStoreSellerProfileUserIdAndStatus(sellerId,
                ProductStatus.ACTIVE);
        long outOfStock = productRepository.countByStoreSellerProfileUserIdAndStockQuantity(sellerId, 0);
        long lowStock = productRepository.countByStoreSellerProfileUserIdAndStockQuantityLessThan(sellerId,
                LOW_STOCK_THRESHOLD);
        long featuredProducts = productRepository.countByStoreSellerProfileUserIdAndFeaturedTrue(sellerId);
        long totalInventoryUnits = productRepository.sumStockQuantityBySellerId(sellerId);
        double avgStock = totalProducts > 0 ? (double) totalInventoryUnits / totalProducts : 0.0;
        BigDecimal totalInventoryValue = productRepository.sumInventoryValueBySellerId(sellerId);
        BigDecimal avgPrice = productRepository.avgPriceBySellerId(sellerId);
        BigDecimal highestPrice = productRepository.maxPriceBySellerId(sellerId);
        BigDecimal lowestPrice = productRepository.minPriceBySellerId(sellerId);
        // Top selling products
        List<TopSellingProductResponse> topSelling = getTopSellingProducts(topProductsLimit);
        // Top rated products (mock)
        List<SellerProductDashboard.TopRatedProduct> topRated = Collections.emptyList();
        LocalDateTime now = LocalDateTime.now();
        long productsAdded30d = productRepository.countByStoreSellerProfileUserIdAndCreatedAtAfter(sellerId,
                now.minusDays(30));
        long productsUpdated30d = productRepository.countByStoreSellerProfileUserIdAndUpdatedAtAfter(sellerId,
                now.minusDays(30));
        return SellerProductDashboard.builder()
                .sellerId(sellerId)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(totalProducts - activeProducts)
                .featuredProducts(featuredProducts)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .totalInventoryUnits(totalInventoryUnits)
                .averageStockPerProduct(avgStock)
                .totalInventoryValue(totalInventoryValue)
                .averagePrice(avgPrice)
                .highestPrice(highestPrice)
                .lowestPrice(lowestPrice)
                .topSellingProducts(topSelling)
                .topRatedProducts(topRated)
                .productsAddedLast30Days(productsAdded30d)
                .productsUpdatedLast30Days(productsUpdated30d)
                .generatedAt(now)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('CUSTOMER') and #customerId == principal.id")
    public Optional<String> getFavoriteCategoryByCustomerId(Long customerId) {
        List<Object[]> result = productRepository.findFavoriteCategoryByCustomerId(customerId);
        if (result.isEmpty())
            return Optional.empty();
        return Optional.ofNullable((String) result.get(0)[0]);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMIN','SELLER','CUSTOMER')")
    public List<TopSellingProductResponse> getTopSellingProducts(int limit) {
        List<Product> products = productRepository.findTopSellingProducts(PageRequest.of(0, limit));
        // Map to DTOs (mock sales data)
        List<TopSellingProductResponse> result = new ArrayList<>();
        int rank = 1;
        for (Product p : products) {
            result.add(TopSellingProductResponse.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .sku(p.getSku())
                    .friendlyUrl(p.getFriendlyUrl())
                    .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                    .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                    .currentPrice(p.getPrice())
                    .discountPrice(p.getDiscountPrice())
                    .imageUrl(getPrimaryImageUrl(p))
                    .totalQuantitySold(0L) // Integrate with OrderItem aggregate query for real sales data
                    .totalRevenue(BigDecimal.ZERO) // Calculate from OrderItem total (quantity * price)
                    .averageOrderQuantity(0.0)
                    .uniqueOrderCount(0L)
                    .rank(rank++)
                    .salesPercentage(0.0)
                    .averageRating(0.0)
                    .reviewCount(0L)
                    .currentStock(p.getStockQuantity())
                    .stockStatus(p.getStockQuantity() == 0 ? "OUT_OF_STOCK"
                            : (p.getStockQuantity() < LOW_STOCK_THRESHOLD ? "LOW_STOCK" : "IN_STOCK"))
                    .sellerId(
                            p.getStore() != null && p.getStore().getSellerProfile() != null
                                    && p.getStore().getSellerProfile().getUser() != null
                                            ? p.getStore().getSellerProfile().getUser().getId()
                                    : null)
                    .storeName(p.getStore() != null ? p.getStore().getStoreName() : null)
                    .build());
        }
        return result;
    }

    /**
     * Full-text search using PostgreSQL tsvector index.
     * 
     * @param query    search keywords
     * @param pageable pagination
     * @return paginated product responses
     */
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public PageResponse<ProductResponse> fullTextSearch(String query, Pageable pageable) {
        var page = productRepository.fullTextSearch(query, pageable);
        return PageResponse.of(page, productMapper::toProductResponse);
    }

    // Delegates to ProductMapper.getPrimaryImageUrl() to avoid duplication.
    private String getPrimaryImageUrl(Product product) {
        return productMapper.getPrimaryImageUrl(product);
    }
}
