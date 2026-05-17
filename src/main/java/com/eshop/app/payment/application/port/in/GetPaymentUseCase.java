package com.eshop.app.payment.application.port.in;

import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Use case for retrieving payment information.
 */
public interface GetPaymentUseCase {
    PaymentResponse getPaymentByTransactionId(String transactionId);
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);
    PageResponse<PaymentResponse> getUserPayments(Long userId, Pageable pageable);
    PageResponse<PaymentResponse> getPaymentsByStatus(PaymentStatus status, Pageable pageable);
    List<PaymentResponse> getFailedPaymentsForRetry();
}



