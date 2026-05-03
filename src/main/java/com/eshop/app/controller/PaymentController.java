package com.eshop.app.controller;

import com.eshop.app.dto.request.PaymentRequest;
import com.eshop.app.dto.request.RefundRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.PaymentResponse;
import com.eshop.app.entity.Payment;
import com.eshop.app.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Controller for handling payment processing and management
 * Provides comprehensive payment gateway integration and transaction management
 * 
 * Security: Role-based access with payment-specific permissions
 * Performance: O(1) operations with caching for frequent queries
 */
@Tag(name = "Payment Management", description = "Payment processing, refunds, and transaction management")
@RestController
@RequestMapping(ApiConstants.Endpoints.PAYMENTS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    @Operation(summary = "Process Payment", 
               description = "Process payment through selected gateway (Stripe, PayPal, etc.)")
    @ApiResponse(responseCode = "201", description = "Payment processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payment request")
    @ApiResponse(responseCode = "402", description = "Payment failed")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PostMapping("/refund")
    @Operation(summary = "Process Refund", 
               description = "Process full or partial refund for a payment")
    @ApiResponse(responseCode = "200", description = "Refund processed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid refund request")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> processRefund(@Valid @RequestBody RefundRequest request) {
        PaymentResponse response = paymentService.processRefund(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get Payment by Transaction ID", 
               description = "Retrieve payment details using transaction ID")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get Payments by Order", 
               description = "Retrieve all payments for a specific order")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        List<PaymentResponse> payments = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Payment History", 
               description = "Retrieve paginated payment history for a user")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> getUserPayments(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PaymentResponse> payments = paymentService.getUserPayments(userId, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/status/{status}")
    @Operation(summary = "Get Payments by Status", 
               description = "Retrieve payments filtered by status (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable Payment.PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PaymentResponse> payments = paymentService.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(payments);
    }
    
    @PostMapping("/verify/{transactionId}")
    @Operation(summary = "Verify Payment", 
               description = "Verify payment status with gateway")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        PaymentResponse response = paymentService.verifyPayment(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/retry/{paymentId}")
    @Operation(summary = "Retry Failed Payment", 
               description = "Retry a failed payment transaction")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> retryPayment(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId) {
        PaymentResponse response = paymentService.retryPayment(paymentId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/failed/retry")
    @Operation(summary = "Get Failed Payments for Retry", 
               description = "Get list of failed payments that can be retried")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponse>> getFailedPaymentsForRetry() {
        List<PaymentResponse> payments = paymentService.getFailedPaymentsForRetry();
        return ResponseEntity.ok(payments);
    }
    
    @GetMapping("/statistics")
    @Operation(summary = "Get Payment Statistics", 
               description = "Get payment statistics for admin dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getPaymentStatistics(
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) LocalDateTime endDate) {
        Object statistics = paymentService.getPaymentStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    @PostMapping("/webhook/{gateway}")
    @Operation(summary = "Payment Webhook Handler", 
               description = "Handle payment webhooks from gateways")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<Void> handlePaymentWebhook(
            @Parameter(description = "Payment gateway") @PathVariable Payment.PaymentGateway gateway,
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        paymentService.handlePaymentWebhook(payload, signature, gateway);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{paymentId}/status")
    @Operation(summary = "Update Payment Status", 
               description = "Update payment status (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId,
            @Parameter(description = "New status") @RequestParam Payment.PaymentStatus status,
            @Parameter(description = "Reason for status change") @RequestParam(required = false) String reason) {
        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, status, reason);
        return ResponseEntity.ok(response);
    }
}