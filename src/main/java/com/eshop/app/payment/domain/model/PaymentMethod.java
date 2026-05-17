package com.eshop.app.payment.domain.model;

/**
 * Specific payment instruments or methods used by a customer.
 */
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
