package com.eshop.app.dto.response;

import com.eshop.app.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment response DTO for API responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    
    private Long id;
    private Long orderId;
    private String transactionId;
    private Payment.PaymentGateway gateway;
    private String gatewayTransactionId;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private Payment.PaymentMethod paymentMethod;
    private String failureReason;
    private LocalDateTime processedAt;
    private BigDecimal refundedAmount;
    private Boolean isRefunded;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Card Payment Details (masked for security)
    private String cardLastFour;
    private String cardBrand;
    private String cardType;
    private String cardIssuerBank;
    private String cardCountry;
    private Boolean isInternationalCard;
    
    // UPI Payment Details
    private String upiId; // Masked UPI ID for display
    private String upiReferenceId;
    private String bankReferenceNumber;
    
    // EMI Details
    private Integer emiTenureMonths;
    private BigDecimal emiAmountPerMonth;
    private BigDecimal emiInterestRate;
    
    // Security Information
    private String authenticationMethod;
    private BigDecimal riskScore;
    
    // Additional fields for client use
    private String paymentUrl; // For redirect-based gateways
    private String qrCode; // For QR code payments
    private String deepLinkUrl; // For UPI deep links
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationResult {
        private Boolean isValid;
        private String message;
        private BigDecimal discountAmount;
        private String errorCode;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationResult {
        private Boolean success;
        private String message;
        private BigDecimal appliedDiscount;
        private BigDecimal finalTotal;
        private String couponCode;
    }
}