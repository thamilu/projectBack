package com.eshop.app.repository;

import com.eshop.app.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        Optional<Order> findByOrderNumber(String orderNumber);

        @EntityGraph(attributePaths = { "items" })
        Page<Order> findByCustomerId(Long customerId, Pageable pageable);

        @EntityGraph(attributePaths = { "items" })
        Page<Order> findByOrderStatus(Order.OrderStatus orderStatus, Pageable pageable);

        @EntityGraph(attributePaths = { "items" })
        Page<Order> findByPaymentStatus(Order.PaymentStatus paymentStatus, Pageable pageable);

        @EntityGraph(attributePaths = { "items" })
        Page<Order> findByDeliveryAgentId(Long deliveryAgentId, Pageable pageable);

        @Query("SELECT o FROM Order o JOIN o.items oi WHERE oi.product.store.id = :storeId")
        Page<Order> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

        @Query("SELECT o FROM Order o JOIN o.customer c LEFT JOIN c.userProfile up WHERE " +
                        "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(c.keycloakId) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(up.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                        "LOWER(up.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
        Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.customer.id = :customerId")
        Long countByCustomerId(@Param("customerId") Long customerId);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.customer.id = :customerId")
        BigDecimal sumTotalAmountByCustomerId(@Param("customerId") Long customerId);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
        Long countOrdersBetweenDates(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal sumRevenueBetweenDates(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
        BigDecimal sumTotalRevenue();

        @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Order o JOIN o.items oi WHERE o.customer.id = :userId AND oi.product.id = :productId AND o.orderStatus = 'DELIVERED'")
        boolean existsByUserIdAndOrderItemsProductId(@Param("userId") Long userId, @Param("productId") Long productId);

        // Dashboard Analytics Methods
        java.util.List<Order> findByDeliveryAgentIdOrderByCreatedAtDesc(Long agentId, Pageable pageable);

        long countByDeliveryAgentIdAndOrderStatus(Long agentId, Order.OrderStatus orderStatus);

        long countByDeliveryAgentIdAndCreatedAtAfter(Long agentId, LocalDateTime createdAt);

        long countByDeliveryAgentIdAndOrderStatusAndCreatedAtAfter(Long agentId, Order.OrderStatus orderStatus,
                        LocalDateTime createdAt);

        long countByDeliveryAgentId(Long agentId);

        long countByOrderStatus(Order.OrderStatus orderStatus);

        @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.items oi WHERE oi.product.store.sellerProfile.user.id = :sellerId AND o.createdAt BETWEEN :startDate AND :endDate")
        BigDecimal sumRevenueBySellerIdBetweenDates(@Param("sellerId") Long sellerId,
                        @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(o) FROM Order o JOIN o.items oi WHERE oi.product.store.sellerProfile.user.id = :sellerId AND o.orderStatus = :status")
        long countByStoreSellerIdAndOrderStatus(@Param("sellerId") Long sellerId,
                        @Param("status") Order.OrderStatus status);

        @EntityGraph(attributePaths = { "items" })
        @Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.product.store.sellerProfile.user.id = :sellerId")
        Page<Order> findByStoreSellerId(@Param("sellerId") Long sellerId, Pageable pageable);

        @EntityGraph(attributePaths = { "items" })
        @Query("SELECT o FROM Order o JOIN o.items oi WHERE oi.product.store.sellerProfile.user.id = :sellerId ORDER BY o.createdAt DESC")
        java.util.List<Order> findRecentOrdersBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);
}
