package com.eshop.app.repository;

import com.eshop.app.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Enhanced ProductRepository with optimized queries.
 * Prevents N+1 queries with proper fetch joins.
 * 
 * @author EShop Team
 * @since 2.0
 */
@Repository
public interface ProductRepositoryEnhanced extends JpaRepository<Product, Long> {
    
    /**
     * CRITICAL-005 FIX: Finds product by ID with pessimistic write lock.
     * Prevents race conditions during stock updates.
     * 
     * <p>Lock Strategy:
     * <ul>
     *   <li>PESSIMISTIC_WRITE - Blocks other transactions until commit</li>
     *   <li>Prevents lost updates in concurrent inventory updates</li>
     *   <li>Database-level lock (SELECT FOR UPDATE)</li>
     * </ul>
     * 
     * <p>Use this method when:
     * <ul>
     *   <li>Updating stock quantity</li>
     *   <li>Processing order items</li>
     *   <li>Reserving inventory</li>
     * </ul>
     * 
     * @param id product ID
     * @return optional product with write lock
     * @throws org.springframework.dao.PessimisticLockingFailureException if lock cannot be acquired
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.deleted = false")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
    
    /**
     * Finds products by seller ID with all associations eagerly loaded.
     * Prevents N+1 queries.
     * 
     * @param sellerId seller ID
     * @param pageable pagination parameters
     * @return page of products
     */
    @Query("""
        SELECT DISTINCT p FROM Product p
        LEFT JOIN FETCH p.category
        LEFT JOIN FETCH p.brand
                LEFT JOIN FETCH p.store s
                LEFT JOIN FETCH s.sellerProfile sp
                LEFT JOIN FETCH sp.user
                WHERE sp.user.id = :sellerId
            AND p.deleted = false
        ORDER BY p.createdAt DESC
        """)
    Page<Product> findBySellerIdWithAssociations(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
    
    /**
     * Gets aggregated product statistics for a seller.
     * Single query aggregation prevents multiple database calls.
     * 
     * @param sellerId seller ID
     * @return map containing totalProducts, activeProducts, averageRating
     */
    @Query("""
        SELECT new map(
            COUNT(p.id) as totalProducts,
            COUNT(CASE WHEN p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE THEN 1 END) as activeProducts,
            AVG(p.averageRating) as averageRating
        )
        FROM Product p
                WHERE p.store.sellerProfile.user.id = :sellerId
            AND p.deleted = false
        """)
    Map<String, Object> getProductStatisticsBySellerId(@Param("sellerId") Long sellerId);
    
    /**
     * Gets overall product statistics for admin dashboard.
     * 
     * @return map containing totalProducts, activeProducts, outOfStockProducts
     */
    @Query("""
        SELECT new map(
            COUNT(p.id) as totalProducts,
            COUNT(CASE WHEN p.status = com.eshop.app.entity.enums.ProductStatus.ACTIVE THEN 1 END) as activeProducts,
            COUNT(CASE WHEN p.stockQuantity = 0 THEN 1 END) as outOfStockProducts
        )
        FROM Product p
        WHERE p.deleted = false
        """)
    Map<String, Object> getProductStatistics();
    
    /**
     * Gets top selling products for a seller.
     * 
     * @param sellerId seller ID
     * @param pageable pagination (use for limit)
     * @return list of top products with sales data
     */
    @Query("""
        SELECT new map(
            p.id as productId,
            p.name as productName,
            p.sku as sku,
            COUNT(oi.id) as orderCount,
            SUM(oi.quantity) as totalSold,
            COALESCE(SUM(oi.subtotal), 0) as revenue
        )
        FROM OrderItem oi
        JOIN oi.product p
        JOIN oi.order o
                WHERE p.store.sellerProfile.user.id = :sellerId
            AND o.orderStatus = 'COMPLETED'
            AND p.deleted = false
        GROUP BY p.id, p.name, p.sku
        ORDER BY SUM(oi.quantity) DESC
        """)
    List<Map<String, Object>> getTopSellingProductsBySellerId(
            @Param("sellerId") Long sellerId,
            Pageable pageable
    );
    
    /**
     * Checks if SKU exists.
     * 
     * @param sku product SKU
     * @return true if exists
     */
    boolean existsBySku(String sku);
    
    /**
     * Finds product by SKU.
     * 
     * @param sku product SKU
     * @return optional product
     */
    Optional<Product> findBySku(String sku);
}
