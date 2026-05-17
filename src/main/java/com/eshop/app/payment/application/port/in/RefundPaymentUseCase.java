package com.eshop.app.payment.application.port.in;

import com.eshop.app.payment.api.request.RefundRequest;
import com.eshop.app.payment.api.response.PaymentResponse;

/**
 * Use case for processing refunds.
 */
public interface RefundPaymentUseCase {
    PaymentResponse processRefund(RefundRequest request);
}
