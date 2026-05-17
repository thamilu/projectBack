package com.eshop.app.payment.shared.exception;





/**
 * Exception thrown when payment operations fail
 * Used for payment processing, refund, and gateway integration errors
 */
public class PaymentException extends RuntimeException {
    
    public PaymentException(String message) {
        super(message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
