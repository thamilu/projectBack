package com.eshop.app.repository;

import com.eshop.app.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Payment repository with optimized queries
 * All queries are O(1) or O(log n) with proper indexing
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Find payment by transaction ID - O(1) with unique index
     */
    Optional<Payment> findByTransactionId(String transactionId);
    
    /**
     * Find payment by gateway transaction ID - O(1) with index
     */
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    
    /**
     * Find payments by order ID - O(log n) with index
     */
    List<Payment> findByOrderId(Long orderId);
    
    /**
     * Find payments by order ID ordered by creation date
     */
    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * Find payments by status - O(log n) with index
     */
    Page<Payment> findByStatus(Payment.PaymentStatus status, Pageable pageable);
    
    /**
     * Find payments by gateway - O(log n) with index
     */
    Page<Payment> findByGateway(Payment.PaymentGateway gateway, Pageable pageable);
    
    /**
     * Find payments by user through order relationship
     */
    @Query("SELECT p FROM Payment p JOIN p.order o WHERE o.customer.id = :userId")
    Page<Payment> findByUserId(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Find failed payments that can be retried
     */
    @Query("SELECT p FROM Payment p WHERE p.status = :status AND p.createdAt >= :since")
    List<Payment> findFailedPaymentsSince(@Param("status") Payment.PaymentStatus status, 
                                         @Param("since") LocalDateTime since);
    
    /**
     * Calculate total revenue for date range
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueBetween(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find all payments between dates for analytics
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    List<Payment> findPaymentsBetweenDates(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);
    
    /**
     * Calculate total revenue by gateway
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.status = 'COMPLETED' AND p.gateway = :gateway")
    BigDecimal calculateRevenueByGateway(@Param("gateway") Payment.PaymentGateway gateway);
    
    /**
     * Find refundable payments for an order
     */
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId " +
           "AND p.status = 'COMPLETED' AND (p.refundedAmount IS NULL OR p.refundedAmount < p.amount)")
    List<Payment> findRefundablePaymentsByOrderId(@Param("orderId") Long orderId);
    
    /**
     * Get payment statistics by gateway
     */
    @Query("SELECT p.gateway, COUNT(p), AVG(p.amount), SUM(p.amount) " +
           "FROM Payment p WHERE p.status = 'COMPLETED' " +
           "GROUP BY p.gateway")
    List<Object[]> getPaymentStatisticsByGateway();
    
    /**
     * Find pending payments older than specified minutes
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' " +
           "AND p.createdAt <= :cutoffTime")
    List<Payment> findPendingPaymentsOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    /**
     * Find payment by gateway reference number and gateway - O(1) with composite index
     */
    Optional<Payment> findByGatewayTransactionIdAndGateway(String gatewayTransactionId, 
                                                          Payment.PaymentGateway gateway);
    
    /**
     * Find payment by UPI transaction ID - O(1) with index
     */
    Optional<Payment> findByUpiTransactionId(String upiTransactionId);
    
    /**
     * Find payments by gateway and status since date - for webhook analysis
     */
    @Query("SELECT p FROM Payment p WHERE p.gateway = :gateway AND p.status = :status AND p.createdAt >= :date")
    List<Payment> findByGatewayAndStatusSince(@Param("gateway") Payment.PaymentGateway gateway,
                                             @Param("status") Payment.PaymentStatus status,
                                             @Param("date") LocalDateTime date);
    
    /**
     * Get payment count by gateway for analytics
     */
    @Query("SELECT p.gateway, COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED' GROUP BY p.gateway")
    List<Object[]> getPaymentCountByGateway();
    
    /**
     * Get payment count by method for analytics
     */
    @Query("SELECT p.paymentMethod, COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED' GROUP BY p.paymentMethod")
    List<Object[]> getPaymentCountByMethod();
}