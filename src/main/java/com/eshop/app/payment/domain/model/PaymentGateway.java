package com.eshop.app.payment.domain.model;

/**
 * Payment gateway options supported by the system.
 */
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
