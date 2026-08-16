package com.eshop.app.payment.application.port.in;

import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.domain.model.PaymentStatus;

/**
 * Use case for processing new payments.
 */
public interface ProcessPaymentUseCase {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse verifyPayment(String transactionId);
    PaymentResponse retryPayment(Long paymentId);
    PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status, String reason);

    // Explicit webhook handlers for gateways to avoid controller business logic leakage
    void handleStripeWebhook(String payload, String signature);
    void handleRazorpayWebhook(String payload, String signature);
    void handlePayUWebhook(String payload);
    void handleCashfreeWebhook(String payload, String signature);
    void handleUpiWebhook(String payload, String signature);
}

