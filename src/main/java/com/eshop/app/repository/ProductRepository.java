package com.eshop.app.repository;   
import com.eshop.app.entity.Product;
import com.eshop.app.repository.projection.ProductDetailProjection;
import com.eshop.app.repository.projection.ProductSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.eshop.app.dto.response.CategorySummaryResponse;
import com.eshop.app.repository.projection.PriceStatsProjection;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
       @EntityGraph(attributePaths = {"tags", "reviews"})
       @Query("SELECT p FROM Product p WHERE p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE AND p.deleted = false")
       Page<Product> findAllWithDetails(Pageable pageable);
    
    // ==================== PROJECTION QUERIES (OPTIMIZED) ====================
    
    /**
     * Find product summary by ID using lightweight projection.
     * Optimized for list views - only fetches required fields.
     */
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = false")
    Optional<ProductSummaryProjection> findSummaryById(@Param("id") Long id);
    
    /**
     * Find all product summaries with pagination.
     * Optimized query for list views.
     */
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE")
    Page<ProductSummaryProjection> findAllSummaries(Pageable pageable);
    
    /**
     * Find product summaries by category.
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.deleted = false AND p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE")
    Page<ProductSummaryProjection> findSummariesByCategory(@Param("categoryId") Long categoryId, Pageable pageable);
    
    /**
     * Find product detail by ID using DTO projection.
     * Optimized for detail view with single query.
     */
    @Query("""
        SELECT new com.eshop.app.repository.projection.ProductDetailProjection(
            p.id, p.name, p.description, p.sku, p.friendlyUrl,
            p.price, p.discountPrice, p.stockQuantity, p.imageUrl,
            p.status, p.featured, p.isMaster,
            c.id, c.name, b.id, b.name, s.id, s.storeName,
            p.createdAt, p.updatedAt, p.version
        )
        FROM Product p
        LEFT JOIN p.category c
        LEFT JOIN p.brand b
        LEFT JOIN p.store s
        WHERE p.id = :id AND p.deleted = false
    """)
    Optional<ProductDetailProjection> findDetailById(@Param("id") Long id);
    
    /**
     * Find product with full relationships for complete DTO conversion.
     * Uses EntityGraph to avoid N+1 queries.
     */
    @EntityGraph(value = "Product.withAllRelations", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findByIdWithRelations(@Param("id") Long id);
    
    // ==================== STANDARD QUERIES ====================
    
    Optional<Product> findBySku(String sku);
    
    Optional<Product> findByFriendlyUrl(String friendlyUrl);
    
    boolean existsBySku(String sku);
    
    boolean existsByFriendlyUrl(String friendlyUrl);
    
    /**
     * Check if product exists for given ID and seller ID (ownership validation).
     * Used by ProductSecurityService for RBAC.
     */
    boolean existsByIdAndStoreSellerProfileUserId(Long productId, Long sellerId);
    
    /**
     * Find product with pessimistic write lock (for stock updates).
     * Prevents concurrent modification during stock transactions.
     * CRITICAL-005 FIX: Pessimistic locking prevents race conditions in stock updates
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithPessimisticLock(@Param("id") Long id);
    
    /**
     * Alias for findByIdWithPessimisticLock for consistent API naming.
     * Used by stock update operations to ensure exclusive write access.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    /**
     * Find product with optimistic lock.
     */
    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithOptimisticLock(@Param("id") Long id);
    
    Page<Product> findByStatus(com.eshop.app.entity.enums.ProductStatus status, Pageable pageable);
    
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    
    Page<Product> findByBrandId(Long brandId, Pageable pageable);
    
    Page<Product> findByStoreId(Long storeId, Pageable pageable);
    
    Page<Product> findByFeatured(Boolean featured, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.stockQuantity > 0")
    Page<Product> findInStockProducts(Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Product> searchProducts(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT p FROM Product p JOIN p.tags t WHERE t.name IN :tagNames")
    Page<Product> findByTagNames(@Param("tagNames") Set<String> tagNames, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.brand.id = :brandId")
    Page<Product> findByCategoryAndBrand(@Param("categoryId") Long categoryId, 
                                         @Param("brandId") Long brandId, 
                                         Pageable pageable);
    
    // Location-based queries with Haversine formula
    @Query(value = """
        SELECT p.*, 
               (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * 
                cos(radians(s.longitude) - radians(:longitude)) + 
                sin(radians(:latitude)) * sin(radians(s.latitude)))) AS distance
        FROM products p
        INNER JOIN stores s ON p.store_id = s.id
        WHERE s.active = true
          AND p.status = 'ACTIVE'
          AND s.latitude IS NOT NULL 
          AND s.longitude IS NOT NULL
          AND (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * 
               cos(radians(s.longitude) - radians(:longitude)) + 
               sin(radians(:latitude)) * sin(radians(s.latitude)))) <= :radiusKm
        ORDER BY distance
        """, 
        countQuery = """
        SELECT COUNT(*)
        FROM products p
        INNER JOIN stores s ON p.store_id = s.id
        WHERE s.active = true
          AND p.status = 'ACTIVE'
          AND s.latitude IS NOT NULL 
          AND s.longitude IS NOT NULL
          AND (6371 * acos(cos(radians(:latitude)) * cos(radians(s.latitude)) * 
               cos(radians(s.longitude) - radians(:longitude)) + 
               sin(radians(:latitude)) * sin(radians(s.latitude)))) <= :radiusKm
        """,
        nativeQuery = true)
    Page<Product> findProductsByLocation(@Param("latitude") Double latitude,
                                         @Param("longitude") Double longitude,
                                         @Param("radiusKm") Double radiusKm,
                                         Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store.city = :city AND p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE")
    Page<Product> findByStoreCity(@Param("city") String city, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store.state = :state AND p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE")
    Page<Product> findByStoreState(@Param("state") String state, Pageable pageable);
    
    @Query("SELECT p FROM Product p WHERE p.store.country = :country AND p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE")
    Page<Product> findByStoreCountry(@Param("country") String country, Pageable pageable);
    
    // Dashboard Analytics Methods
    long countByStoreSellerProfileUserId(Long sellerId);

    long countByStoreSellerProfileUserIdAndStatus(Long sellerId, com.eshop.app.entity.enums.ProductStatus status);

    long countByStoreSellerProfileUserIdAndStockQuantity(Long sellerId, Integer stockQuantity);

    long countByStoreSellerProfileUserIdAndStockQuantityLessThan(Long sellerId, Integer stockQuantity);
       long countByStatus(com.eshop.app.entity.enums.ProductStatus status);
       long countByFeatured(Boolean featured);
       long countByStockQuantity(int stockQuantity);
       long countByStockQuantityLessThan(int stockQuantity);

       @Query("SELECT COALESCE(SUM(p.stockQuantity),0) FROM Product p")
       Long sumStockQuantity();

       @Query("SELECT COALESCE(AVG(p.price),0) FROM Product p")
       BigDecimal avgPrice();

       @Query("SELECT COALESCE(MIN(p.price),0) FROM Product p")
       BigDecimal minPrice();

       @Query("SELECT COALESCE(MAX(p.price),0) FROM Product p")
       BigDecimal maxPrice();

       @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity),0) FROM Product p")
       BigDecimal sumInventoryValue();

       long countByCreatedAtAfter(LocalDateTime dt);

       long countByUpdatedAtAfter(LocalDateTime dt);

       long countByStoreSellerProfileUserIdAndFeaturedTrue(Long sellerId);

       @Query("SELECT COALESCE(SUM(p.stockQuantity),0) FROM Product p WHERE p.store.sellerProfile.user.id = :sellerId")
       Long sumStockQuantityBySellerId(@Param("sellerId") Long sellerId);

       @Query("SELECT COALESCE(SUM(p.price * p.stockQuantity),0) FROM Product p WHERE p.store.sellerProfile.user.id = :sellerId")
       BigDecimal sumInventoryValueBySellerId(@Param("sellerId") Long sellerId);

       @Query("SELECT COALESCE(AVG(p.price),0) FROM Product p WHERE p.store.sellerProfile.user.id = :sellerId")
       BigDecimal avgPriceBySellerId(@Param("sellerId") Long sellerId);

       @Query("SELECT COALESCE(MAX(p.price),0) FROM Product p WHERE p.store.sellerProfile.user.id = :sellerId")
       BigDecimal maxPriceBySellerId(@Param("sellerId") Long sellerId);

       @Query("SELECT COALESCE(MIN(p.price),0) FROM Product p WHERE p.store.sellerProfile.user.id = :sellerId")
       BigDecimal minPriceBySellerId(@Param("sellerId") Long sellerId);

    long countByStoreSellerProfileUserIdAndCreatedAtAfter(Long sellerId, LocalDateTime dt);

    long countByStoreSellerProfileUserIdAndUpdatedAtAfter(Long sellerId, LocalDateTime dt);
 
    @Query("SELECT c.name, COUNT(oi.id) FROM OrderItem oi JOIN oi.product p JOIN p.category c WHERE oi.order.customer.id = :customerId GROUP BY c.name ORDER BY COUNT(oi.id) DESC")
    java.util.List<Object[]> findFavoriteCategoryByCustomerId(@Param("customerId") Long customerId);
 
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.store s LEFT JOIN FETCH s.sellerProfile sp LEFT JOIN FETCH sp.user ORDER BY p.createdAt DESC")
    java.util.List<Product> findTopSellingProducts(Pageable pageable);
 
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.store s LEFT JOIN FETCH s.sellerProfile sp LEFT JOIN FETCH sp.user WHERE sp.user.id = :sellerId ORDER BY p.createdAt DESC")
    java.util.List<Product> findTopSellingProductsBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
 
    java.util.List<Product> findByStoreSellerProfileUserId(Long sellerId);

       // Brand related stats used by BrandServiceImpl
       long countByBrandId(Long brandId);

       long countByBrandIdAndStatus(Long brandId, com.eshop.app.entity.enums.ProductStatus status);

       @Query("SELECT AVG(p.price) as averagePrice, MIN(p.price) as minPrice, MAX(p.price) as maxPrice FROM Product p WHERE p.brand.id = :brandId")
       PriceStatsProjection findPriceStatsByBrandIdRaw(@Param("brandId") Long brandId);

       default java.util.Optional<PriceStatsProjection> findPriceStatsByBrandId(Long brandId) {
              PriceStatsProjection proj = findPriceStatsByBrandIdRaw(brandId);
              return java.util.Optional.ofNullable(proj);
       }

       @Query("SELECT c.id as id, c.name as name, c.slug as slug, COUNT(p.id) as productCount FROM Product p JOIN p.category c WHERE p.brand.id = :brandId GROUP BY c.id, c.name, c.slug")
       List<Object[]> findCategorySummariesRawByBrandId(@Param("brandId") Long brandId);

       default List<CategorySummaryResponse> findCategorySummariesByBrandId(Long brandId) {
              List<Object[]> rows = findCategorySummariesRawByBrandId(brandId);
              return rows.stream().map(r -> new CategorySummaryResponse(((Number) r[0]).longValue(), (String) r[1], (String) r[2], ((Number) r[3]).longValue())).collect(Collectors.toList());
       }

       @Query("SELECT p.brand.id, COUNT(p.id) FROM Product p WHERE p.brand.id IN :ids GROUP BY p.brand.id")
       List<Object[]> countByBrandIdsRaw(@Param("ids") Set<Long> ids);

       default Map<Long, Long> countByBrandIds(Set<Long> ids) {
              List<Object[]> rows = countByBrandIdsRaw(ids);
              return rows.stream().collect(Collectors.toMap(r -> ((Number) r[0]).longValue(), r -> ((Number) r[1]).longValue()));
       }


/**
     * Full-text search using PostgreSQL tsvector index.
     * Matches against name, description, and SKU.
     */
    @Query(value = "SELECT * FROM products WHERE search_vector @@ plainto_tsquery('english', :query) AND deleted = false",
           countQuery = "SELECT count(*) FROM products WHERE search_vector @@ plainto_tsquery('english', :query) AND deleted = false",
           nativeQuery = true)
    Page<Product> fullTextSearch(@Param("query") String query, Pageable pageable);




}


