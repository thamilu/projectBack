package com.eshop.app.service;

import com.eshop.app.dto.request.*;
import com.eshop.app.dto.response.*;
import com.eshop.app.exception.ProductNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.Pageable;

/**
 * Product management service.
 * 
 * <p>Implementations should:
 * <ul>
 *   <li>Apply caching for read operations</li>
 *   <li>Publish domain events for mutations</li>
 *   <li>Enforce authorization via @PreAuthorize</li>
 * </ul>
 */
public interface ProductService {
    
    // ═══════════════════════════════════════════════════════════════
    // CONSTANTS
    // ═══════════════════════════════════════════════════════════════
    int MAX_BATCH_SIZE = 100;
    int MAX_TOP_PRODUCTS = 50;
    
    // ═══════════════════════════════════════════════════════════════
    // CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    ProductResponse createProduct(@Valid @NotNull ProductCreateRequest request, String userId);
    
    ProductResponse createProductWithAutoCategory(
        @Valid @NotNull ProductCreateWithCategoryRequest request
    );
    
    ProductResponse updateProduct(
        @NotNull @Positive Long id, 
        @Valid @NotNull ProductUpdateRequest request
    );
    
    void deleteProduct(@NotNull @Positive Long id);
    
    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    Optional<ProductResponse> findProductById(@NotNull @Positive Long id);
    
    default ProductResponse getProductById(@NotNull @Positive Long id) {
        return findProductById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
    }
    
    Optional<ProductResponse> findProductBySku(@NotBlank String sku);
    
    Optional<ProductResponse> findProductByFriendlyUrl(@NotBlank String friendlyUrl);
    
    Map<Long, ProductResponse> getProductsByIds(@NotEmpty @Size(max = MAX_BATCH_SIZE) Set<Long> ids);
    
    // ═══════════════════════════════════════════════════════════════
    // SEARCH & FILTER
    // ═══════════════════════════════════════════════════════════════
    
    PageResponse<ProductResponse> searchProducts(
        @Valid ProductSearchCriteria criteria, 
        @NotNull Pageable pageable
    );
    
    PageResponse<ProductResponse> getFeaturedProducts(@NotNull Pageable pageable);
    
    // ═══════════════════════════════════════════════════════════════
    // ANALYTICS
    // ═══════════════════════════════════════════════════════════════
    
    ProductStatistics getGlobalStatistics();
    
    SellerProductDashboard getSellerDashboard(
        @NotNull @Positive Long sellerId,
        @Min(1) @Max(MAX_TOP_PRODUCTS) int topProductsLimit
    );
    
    Optional<String> getFavoriteCategoryByCustomerId(@NotNull @Positive Long customerId);
    
    List<TopSellingProductResponse> getTopSellingProducts(
        @Min(1) @Max(MAX_TOP_PRODUCTS) int limit
    );
    
    // Additional convenience query methods used by controllers/services
    ProductResponse getProductBySku(@NotBlank String sku);

    ProductResponse getProductByFriendlyUrl(@NotBlank String friendlyUrl);

    PageResponse<ProductListResponse> getAllProducts(@NotNull Pageable pageable);

    PageResponse<ProductListResponse> getProductsByCategory(@NotNull @Positive Long categoryId, @NotNull Pageable pageable);

    PageResponse<ProductListResponse> getProductsByBrand(@NotNull @Positive Long brandId, @NotNull Pageable pageable);

    PageResponse<ProductListResponse> getProductsByStore(@NotNull @Positive Long storeId, @NotNull Pageable pageable);

    PageResponse<ProductListResponse> searchProducts(String keyword, @NotNull Pageable pageable);

    PageResponse<ProductResponse> getProductsByTags(@NotEmpty Set<String> tags, @NotNull Pageable pageable);
    
    // ═══════════════════════════════════════════════════════════════
    // BATCH OPERATIONS
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Create multiple products in batch.
     * @param request Batch request with products and options
     * @return Result with successful and failed operations
     */
    BatchOperationResult<ProductResponse> createProductsBatch(@Valid @NotNull BatchProductCreateRequest request);
    
    /**
     * Delete multiple products in batch.
     * @param ids Product IDs to delete
     * @param userId User performing the deletion
     * @return Result with successful and failed deletions
     */
    BatchOperationResult<Long> deleteProductsBatch(
        @NotEmpty @Size(max = MAX_BATCH_SIZE) List<Long> ids, 
        @NotBlank String userId
    );
    
    // ═══════════════════════════════════════════════════════════════
    // INVENTORY
    // ═══════════════════════════════════════════════════════════════
    
    /**
     * Update stock with operation type (SET, INCREMENT, DECREMENT).
     * @param id Product ID
     * @param request Stock update request with operation and quantity
     * @return Updated product
     */
    ProductResponse updateStockAndReturn(@NotNull @Positive Long id, @Valid @NotNull StockUpdateRequest request);
    
    void updateStock(@NotNull @Positive Long productId, @NotNull @PositiveOrZero Integer quantity);
    
    void adjustStock(@NotNull @Positive Long productId, int delta);
    
    default void updateStockBatch(@NotEmpty @Size(max = MAX_BATCH_SIZE) Map<Long, Integer> stockUpdates) {
        throw new UnsupportedOperationException("updateStockBatch not implemented");
    }

    // Dashboard / analytics helpers
    long getTotalProductCount();

    long getProductCountBySellerId(@NotNull @Positive Long sellerId);

    long getActiveProductCountBySellerId(@NotNull @Positive Long sellerId);

    long getOutOfStockCountBySellerId(@NotNull @Positive Long sellerId);

    long getLowStockCountBySellerId(@NotNull @Positive Long sellerId);

    List<Map<String, Object>> getProductPerformanceBySellerId(@NotNull @Positive Long sellerId);

    default List<TopSellingProductResponse> getTopSellingProductsBySellerId(@NotNull @Positive Long sellerId, @Min(1) @Max(MAX_TOP_PRODUCTS) int limit) {
        return getTopSellingProducts(limit);
    }
    /**
     * Full-text search using PostgreSQL tsvector index.
     * @param query search keywords
     * @param pageable pagination
     * @return paginated product responses
     */
    PageResponse<ProductResponse> fullTextSearch(String query, org.springframework.data.domain.Pageable pageable);
}