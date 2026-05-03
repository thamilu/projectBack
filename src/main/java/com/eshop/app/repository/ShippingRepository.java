package com.eshop.app.repository;

import com.eshop.app.entity.Shipping;
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
 * Shipping repository with optimized queries for logistics management
 * All queries are O(1) or O(log n) with proper indexing
 */
@Repository
public interface ShippingRepository extends JpaRepository<Shipping, Long> {
    
    /**
     * Find shipping by order ID - O(1) with unique constraint
     */
    Optional<Shipping> findByOrderId(Long orderId);
    
    /**
     * Find shipping by tracking number - O(1) with unique index
     */
    Optional<Shipping> findByTrackingNumber(String trackingNumber);
    
    /**
     * Find shippings by status - O(log n) with index
     */
    Page<Shipping> findByStatus(Shipping.ShippingStatus status, Pageable pageable);
    
    /**
     * Find shippings by carrier - O(log n) with index
     */
    Page<Shipping> findByCarrier(Shipping.ShippingCarrier carrier, Pageable pageable);
    
    /**
     * Find shippings by method
     */
    Page<Shipping> findByMethod(Shipping.ShippingMethod method, Pageable pageable);
    
    /**
     * Find shippings for a user through order relationship
     */
    @Query("SELECT s FROM Shipping s JOIN s.order o WHERE o.customer.id = :userId")
    Page<Shipping> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find shippings that are overdue for delivery
     */
    @Query("SELECT s FROM Shipping s WHERE s.estimatedDeliveryDate < :now " +
           "AND s.status NOT IN ('DELIVERED', 'RETURNED', 'LOST')")
    List<Shipping> findOverdueDeliveries(@Param("now") LocalDateTime now);
    
    /**
     * Find shippings by estimated delivery date range
     */
    @Query("SELECT s FROM Shipping s WHERE s.estimatedDeliveryDate BETWEEN :startDate AND :endDate")
    Page<Shipping> findByEstimatedDeliveryDateBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);
    
    /**
     * Find in-transit shippings
     */
    @Query("SELECT s FROM Shipping s WHERE s.status IN ('SHIPPED', 'IN_TRANSIT', 'OUT_FOR_DELIVERY')")
    Page<Shipping> findInTransitShippings(Pageable pageable);
    
    /**
     * Find shippings by carrier and status
     */
    Page<Shipping> findByCarrierAndStatus(Shipping.ShippingCarrier carrier, 
                                         Shipping.ShippingStatus status, 
                                         Pageable pageable);
    
    /**
     * Count deliveries by status
     */
    @Query("SELECT s.status, COUNT(s) FROM Shipping s GROUP BY s.status")
    List<Object[]> countByStatus();
    
    /**
     * Count deliveries by carrier
     */
    @Query("SELECT s.carrier, COUNT(s) FROM Shipping s GROUP BY s.carrier")
    List<Object[]> countByCarrier();
    
    /**
     * Find shippings that need tracking update
     * (shipped but not updated in last 24 hours)
     */
    @Query("SELECT s FROM Shipping s WHERE s.status IN ('SHIPPED', 'IN_TRANSIT') " +
           "AND s.updatedAt <= :cutoffTime")
    List<Shipping> findShippingsNeedingTrackingUpdate(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Get delivery performance metrics by carrier
     */
    @Query("SELECT s.carrier, " +
           "COUNT(CASE WHEN s.actualDeliveryDate <= s.estimatedDeliveryDate THEN 1 END), " +
           "COUNT(s) " +
           "FROM Shipping s WHERE s.status = 'DELIVERED' " +
           "GROUP BY s.carrier")
    List<Object[]> getDeliveryPerformanceByCarrier();
}