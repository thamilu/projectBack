package com.eshop.app.payment.domain.model;

/**
 * Lifecycle states of a payment transaction.
 */
public enum PaymentStatus {
    PENDING,           // Payment initiated but not processed
    PROCESSING,        // Payment being processed
    COMPLETED,         // Payment successful
    FAILED,           // Payment failed
    CANCELLED,        // Payment cancelled by user
    REFUNDED,         // Payment refunded
    PARTIALLY_REFUNDED // Payment partially refunded
}
