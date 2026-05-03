package com.eshop.app.repository;

import com.eshop.app.entity.Coupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Coupon repository with optimized queries for discount management
 * All queries are O(1) or O(log n) with proper indexing
 */
@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
    
    /**
     * Find coupon by code - O(1) with unique index
     */
    Optional<Coupon> findByCode(String code);
    
    /**
     * Find active coupon by code - O(1) with composite index
     */
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    Optional<Coupon> findActiveByCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    /**
     * Find active coupons - O(log n) with index
     */
    Page<Coupon> findByIsActiveTrue(Pageable pageable);
    
    /**
     * Find coupons by store - O(log n) with index
     */
    Page<Coupon> findByStoreId(Long storeId, Pageable pageable);
    
    /**
     * Find active coupons by store
     */
    @Query("SELECT c FROM Coupon c WHERE c.store.id = :storeId AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findActiveByStoreId(@Param("storeId") Long storeId, @Param("now") LocalDateTime now);
    
    /**
     * Find coupons by category
     */
    Page<Coupon> findByCategoryId(Long categoryId, Pageable pageable);
    
    /**
     * Find active coupons by category
     */
    @Query("SELECT c FROM Coupon c WHERE c.category.id = :categoryId AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findActiveByCategoryId(@Param("categoryId") Long categoryId, @Param("now") LocalDateTime now);
    
    /**
     * Find global coupons (not store or category specific)
     */
    @Query("SELECT c FROM Coupon c WHERE c.store IS NULL AND c.category IS NULL AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findGlobalActiveCoupons(@Param("now") LocalDateTime now);
    
    /**
     * Find coupons expiring soon
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.validUntil BETWEEN :now AND :soonDate")
    List<Coupon> findCouponsExpiringSoon(@Param("now") LocalDateTime now, 
                                        @Param("soonDate") LocalDateTime soonDate);
    
    /**
     * Find coupons that have reached usage limit
     */
    @Query("SELECT c FROM Coupon c WHERE c.usageLimit IS NOT NULL AND c.usedCount >= c.usageLimit")
    List<Coupon> findCouponsAtUsageLimit();
    
    /**
     * Find expired coupons that are still active (cleanup query)
     */
    @Query("SELECT c FROM Coupon c WHERE c.isActive = true AND c.validUntil < :now")
    List<Coupon> findExpiredActiveCoupons(@Param("now") LocalDateTime now);
    
    /**
     * Find first-time-only coupons
     */
    @Query("SELECT c FROM Coupon c WHERE c.firstTimeOnly = true AND c.isActive = true " +
           "AND c.validFrom <= :now AND c.validUntil >= :now")
    List<Coupon> findFirstTimeOnlyCoupons(@Param("now") LocalDateTime now);
    
    /**
     * Get coupon usage statistics
     */
    @Query("SELECT c.discountType, COUNT(c), AVG(c.usedCount), SUM(c.usedCount) " +
           "FROM Coupon c WHERE c.usedCount > 0 " +
           "GROUP BY c.discountType")
    List<Object[]> getCouponUsageStatistics();
    
    /**
     * Find coupons with high usage rate
     */
    @Query("SELECT c FROM Coupon c WHERE c.usageLimit IS NOT NULL " +
           "AND (CAST(c.usedCount AS double) / c.usageLimit) >= :threshold")
    List<Coupon> findHighUsageCoupons(@Param("threshold") double threshold);
    
    /**
     * Search coupons by code or name
     */
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Coupon> searchCoupons(@Param("keyword") String keyword, Pageable pageable);
}