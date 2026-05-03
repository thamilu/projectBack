package com.eshop.app.dto.request;

import com.eshop.app.entity.Payment;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payment request DTO for processing payments
 * Contains all necessary information for payment processing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Payment gateway is required")
    private Payment.PaymentGateway gateway;
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Amount format is invalid")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Builder.Default
    private String currency = "USD";
    
    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
    
    // Gateway-specific fields
    private String stripeToken;
    private String stripePaymentMethodId;
    private String paypalPaymentId;
    private String paypalPayerId;
    
    // Indian Payment Gateway Fields
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;
    private String payuTxnId;
    private String cashfreeOrderId;
    
    // UPI Payment Fields
    private UpiInfo upiInfo;
    
    // Card information (for direct processing)
    private CardInfo cardInfo;
    
    // EMI Information
    private EmiInfo emiInfo;
    
    // Return URLs (for redirecting gateways)
    private String returnUrl;
    private String cancelUrl;
    
    // Additional metadata
    private String description;
    private String customerIp;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardInfo {
        @NotBlank(message = "Card number is required")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid card number format")
        private String cardNumber;
        
        @NotBlank(message = "Expiry month is required")
        @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Invalid expiry month")
        private String expiryMonth;
        
        @NotBlank(message = "Expiry year is required")
        @Pattern(regexp = "^\\d{4}$", message = "Invalid expiry year")
        private String expiryYear;
        
        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^\\d{3,4}$", message = "Invalid CVV")
        private String cvv;
        
        @NotBlank(message = "Cardholder name is required")
        @Size(max = 100, message = "Cardholder name too long")
        private String cardholderName;
        
        // Additional card fields
        private String cardBrand; // Auto-detected: VISA, MASTERCARD, etc.
        private String cardType;  // CREDIT, DEBIT
        @Builder.Default
        private Boolean saveCard = false; // Save for future use
        private String savedCardToken; // For saved card payments
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpiInfo {
        @NotBlank(message = "UPI ID is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$", message = "Invalid UPI ID format")
        private String upiId; // Virtual Payment Address (VPA)
        
        private String upiApp; // GOOGLEPAY, PHONEPE, PAYTM, etc.
        private String deviceFingerprint;
        private String customerReference;
        
        // For UPI Collect (Request money flow)
        @Builder.Default
        private Integer expiryMinutes = 15; // Request expiry time
        private String description;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmiInfo {
        @NotNull(message = "EMI tenure is required")
        @Min(value = 3, message = "EMI tenure must be at least 3 months")
        @Max(value = 60, message = "EMI tenure cannot exceed 60 months")
        private Integer tenureMonths;
        
        @NotNull(message = "Interest rate is required")
        @DecimalMin(value = "0.0", message = "Interest rate must be non-negative")
        @DecimalMax(value = "50.0", message = "Interest rate cannot exceed 50%")
        private BigDecimal interestRate;
        
        @NotNull(message = "EMI amount is required")
        private BigDecimal monthlyAmount;
        
        private String bankName;
        private String emiProvider; // BAJAJ, ZESTMONEY, etc.
        @Builder.Default
        private Boolean preApproved = false;
    }
}