package com.eshop.app.service;

import com.eshop.app.dto.request.PaymentRequest;
import com.eshop.app.dto.request.RefundRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.PaymentResponse;
import com.eshop.app.entity.Payment;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment service interface for handling payment processing
 * Supports multiple payment gateways with unified API
 * 
 * Time Complexity: All operations O(1) or O(log n)
 * Space Complexity: O(1) for processing, O(n) for batch operations
 */
public interface PaymentService {
    
    /**
     * Process payment through specified gateway
     * 
     * @param request Payment details including amount, gateway, order info
     * @return Payment response with transaction details
     * 
     * Business Rules:
     * - Validates order exists and is in valid state
     * - Processes payment through selected gateway
     * - Updates order payment status
     * - Handles payment failures gracefully
     */
    PaymentResponse processPayment(PaymentRequest request);
    
    /**
     * Get payment by transaction ID
     */
    PaymentResponse getPaymentByTransactionId(String transactionId);
    
    /**
     * Get payments for an order
     */
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);
    
    /**
     * Get user's payment history
     */
    PageResponse<PaymentResponse> getUserPayments(Long userId, Pageable pageable);
    
    /**
     * Get payments by status
     */
    PageResponse<PaymentResponse> getPaymentsByStatus(Payment.PaymentStatus status, Pageable pageable);
    
    /**
     * Process refund for a payment
     * 
     * @param request Refund details including payment ID and amount
     * @return Updated payment response
     * 
     * Business Rules:
     * - Only completed payments can be refunded
     * - Partial refunds allowed up to original amount
     * - Updates order status if fully refunded
     */
    PaymentResponse processRefund(RefundRequest request);
    
    /**
     * Verify payment with gateway
     * Used for webhook validation and status sync
     */
    PaymentResponse verifyPayment(String transactionId);
    
    /**
     * Handle payment webhook from gateway
     */
    void handlePaymentWebhook(String payload, String signature, Payment.PaymentGateway gateway);
    
    /**
     * Get payment statistics for admin dashboard
     */
    Object getPaymentStatistics(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Calculate total revenue for period
     */
    BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Get failed payments that need retry
     */
    List<PaymentResponse> getFailedPaymentsForRetry();
    
    /**
     * Retry failed payment
     */
    PaymentResponse retryPayment(Long paymentId);
    
    /**
     * Update payment status (for admin use)
     */
    PaymentResponse updatePaymentStatus(Long paymentId, Payment.PaymentStatus status, String reason);
}