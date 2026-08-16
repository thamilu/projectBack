package com.eshop.app.payment.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.payment.api.request.RefundRequest;
import com.eshop.app.core.api.response.PageResponse;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Payment Controller for handling payment processing and management
 * Provides comprehensive payment gateway integration and transaction management
 * 
 * Security: Role-based access with payment-specific permissions
 * Performance: O(1) operations with caching for frequent queries
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Payment Management", description = "Payment processing, refunds, and transaction management")
@RestController
@RequestMapping(ApiConstants.Endpoints.PAYMENTS)
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class PaymentController {

    private final com.eshop.app.payment.application.port.in.ProcessPaymentUseCase processPaymentUseCase;
    private final com.eshop.app.payment.application.port.in.GetPaymentUseCase getPaymentUseCase;
    private final com.eshop.app.payment.application.port.in.RefundPaymentUseCase refundPaymentUseCase;
    private final com.eshop.app.payment.application.port.in.PaymentAnalyticsUseCase paymentAnalyticsUseCase;

    @PostMapping("/process")
    @Operation(summary = "Process Payment", description = "Process payment through selected gateway (Stripe, PayPal, etc.)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment processed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payment request")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "402", description = "Payment failed")
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = processPaymentUseCase.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/refund")
    @Operation(summary = "Process Refund", description = "Process full or partial refund for a payment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Refund processed successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid refund request")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ResponseEntity<PaymentResponse> processRefund(@Valid @RequestBody RefundRequest request) {
        PaymentResponse response = refundPaymentUseCase.processRefund(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get Payment by Transaction ID", description = "Retrieve payment details using transaction ID")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        PaymentResponse response = getPaymentUseCase.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get Payments by Order", description = "Retrieve all payments for a specific order")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        List<PaymentResponse> payments = getPaymentUseCase.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Payment History", description = "Retrieve paginated payment history for a user")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<PageResponse<PaymentResponse>> getUserPayments(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PaymentResponse> payments = getPaymentUseCase.getUserPayments(userId, pageable);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get Payments by Status", description = "Retrieve payments filtered by status (admin only)")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<PageResponse<PaymentResponse>> getPaymentsByStatus(
            @Parameter(description = "Payment status") @PathVariable PaymentStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PaymentResponse> payments = getPaymentUseCase.getPaymentsByStatus(status, pageable);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/verify/{transactionId}")
    @Operation(summary = "Verify Payment", description = "Verify payment status with gateway")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    public ResponseEntity<PaymentResponse> verifyPayment(
            @Parameter(description = "Transaction ID") @PathVariable String transactionId) {
        PaymentResponse response = processPaymentUseCase.verifyPayment(transactionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/retry/{paymentId}")
    @Operation(summary = "Retry Failed Payment", description = "Retry a failed payment transaction")
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    public ResponseEntity<PaymentResponse> retryPayment(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId) {
        PaymentResponse response = processPaymentUseCase.retryPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/failed/retry")
    @Operation(summary = "Get Failed Payments for Retry", description = "Get list of failed payments that can be retried")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<List<PaymentResponse>> getFailedPaymentsForRetry() {
        List<PaymentResponse> payments = getPaymentUseCase.getFailedPaymentsForRetry();
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get Payment Statistics", description = "Get payment statistics for admin dashboard")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<Object> getPaymentStatistics(
            @Parameter(description = "Start date (ISO format)") @RequestParam(required = false) LocalDateTime startDate,
            @Parameter(description = "End date (ISO format)") @RequestParam(required = false) LocalDateTime endDate) {
        Object statistics = paymentAnalyticsUseCase.getPaymentStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }


    @PutMapping("/{paymentId}/status")
    @Operation(summary = "Update Payment Status", description = "Update payment status (admin only)")
    @PreAuthorize(IS_ADMIN)
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId,
            @Parameter(description = "New status") @RequestParam PaymentStatus status,
            @Parameter(description = "Reason for status change") @RequestParam(required = false) String reason) {
        PaymentResponse response = processPaymentUseCase.updatePaymentStatus(paymentId, status, reason);
        return ResponseEntity.ok(response);
    }
}


