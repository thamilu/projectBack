package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity representing payment transactions in the system
 * Supports multiple payment gateways (Stripe, PayPal, etc.)
 * 
 * Time Complexity: O(1) for all operations with proper indexing
 * Space Complexity: O(1) per payment record
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order_id", columnList = "order_id"),
    @Index(name = "idx_payment_transaction_id", columnList = "transaction_id", unique = true),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_gateway", columnList = "gateway"),
    @Index(name = "idx_payment_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @Column(name = "transaction_id", unique = true, length = 100)
    private String transactionId;
    
    @Column(name = "gateway", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentGateway gateway;
    
    @Column(name = "gateway_transaction_id", length = 100)
    private String gatewayTransactionId;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    
    @Column(name = "payment_method", length = 50)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;
    
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "response_code", length = 50)
    private String responseCode;
    
    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;
    
    // Card Payment Details
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;
    
    @Column(name = "card_brand", length = 20)
    private String cardBrand; // VISA, MASTERCARD, AMEX, etc.
    
    @Column(name = "card_type", length = 10)
    private String cardType; // CREDIT, DEBIT
    
    @Column(name = "card_issuer_bank", length = 100)
    private String cardIssuerBank;
    
    @Column(name = "card_country", length = 3)
    private String cardCountry; // ISO country code
    
    // UPI Payment Details
    @Column(name = "upi_id", length = 100)
    private String upiId; // Virtual Payment Address (VPA)
    
    @Column(name = "upi_reference_id", length = 100)
    private String upiReferenceId; // UPI transaction reference
    
    @Column(name = "upi_transaction_id", length = 100)
    private String upiTransactionId; // UPI transaction ID from gateway
    
    @Column(name = "bank_reference_number", length = 50)
    private String bankReferenceNumber; // Bank transaction reference
    
    // EMI Details
    @Column(name = "emi_tenure_months")
    private Integer emiTenureMonths;
    
    @Column(name = "emi_amount_per_month", precision = 10, scale = 2)
    private BigDecimal emiAmountPerMonth;
    
    @Column(name = "emi_interest_rate", precision = 5, scale = 2)
    private BigDecimal emiInterestRate;
    
    // Additional Security Fields
    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore; // Risk assessment score
    
    @Column(name = "is_international_card", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isInternationalCard = false;
    
    @Column(name = "authentication_method", length = 20)
    private String authenticationMethod; // 3DS, OTP, PIN, etc.
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @Column(name = "failed_at")
    private LocalDateTime failedAt;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "refunded_amount", precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;
    
    @Column(name = "is_refunded", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isRefunded = false;
    
    // Payment gateway enums
    public enum PaymentGateway {
        STRIPE,              // Credit/Debit Cards (International)
        PAYPAL,              // PayPal Wallet
        RAZORPAY,            // Credit/Debit Cards + UPI (India)
        PAYU,                // Credit/Debit Cards + UPI + Wallets (India)
        CASHFREE,            // Credit/Debit Cards + UPI + Wallets (India)
        PHONEPE,             // UPI + Wallets (India)
        GOOGLEPAY,           // UPI + Cards (India)
        PAYTM,               // UPI + Wallets + Cards (India)
        BANK_TRANSFER,       // Direct Bank Transfer
        CASH_ON_DELIVERY     // COD
    }
    
    public enum PaymentStatus {
        PENDING,           // Payment initiated but not processed
        PROCESSING,        // Payment being processed
        COMPLETED,         // Payment successful
        FAILED,           // Payment failed
        CANCELLED,        // Payment cancelled by user
        REFUNDED,         // Payment refunded
        PARTIALLY_REFUNDED // Payment partially refunded
    }
    
    public enum PaymentMethod {
        // Card Payments
        CREDIT_CARD_VISA,
        CREDIT_CARD_MASTERCARD,
        CREDIT_CARD_AMERICAN_EXPRESS,
        CREDIT_CARD_DISCOVER,
        CREDIT_CARD_RUPAY,        // India specific
        DEBIT_CARD_VISA,
        DEBIT_CARD_MASTERCARD,
        DEBIT_CARD_RUPAY,         // India specific
        DEBIT_CARD_MAESTRO,
        
        // UPI Payments (India)
        UPI,                      // Generic UPI
        UPI_GOOGLEPAY,           // Google Pay
        UPI_PHONEPE,             // PhonePe
        UPI_PAYTM,               // Paytm UPI
        UPI_AMAZON_PAY,          // Amazon Pay UPI
        UPI_BHIM,                // BHIM UPI
        UPI_WHATSAPP,            // WhatsApp Pay
        
        // Digital Wallets
        PAYPAL,
        PAYTM_WALLET,
        PHONEPE_WALLET,
        AMAZON_PAY_WALLET,
        MOBIKWIK,
        FREECHARGE,
        
        // Bank Transfers
        BANK_TRANSFER,
        NET_BANKING,
        IMPS,
        NEFT,
        RTGS,
        
        // Other Methods
        CASH_ON_DELIVERY,
        EMI,                     // Equated Monthly Installments
        BUY_NOW_PAY_LATER       // BNPL services
    }
    
    /**
     * Check if payment can be refunded
     * Business Rule: Only completed payments can be refunded
     */
    public boolean canBeRefunded() {
        return status == PaymentStatus.COMPLETED && 
               (refundedAmount == null || refundedAmount.compareTo(amount) < 0);
    }
    
    /**
     * Calculate remaining refundable amount
     */
    public BigDecimal getRefundableAmount() {
        if (!canBeRefunded()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount != null ? refundedAmount : BigDecimal.ZERO);
    }
    
    /**
     * Mark payment as processed
     */
    public void markAsProcessed(PaymentStatus status, String gatewayTransactionId) {
        this.status = status;
        this.gatewayTransactionId = gatewayTransactionId;
        this.processedAt = LocalDateTime.now();
    }
}