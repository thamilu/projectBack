package com.eshop.app.payment.application.service;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * Outcome of a single payment-gateway operation (charge, capture, or refund).
 *
 * <p>Extracted as a top-level type so it can be shared between {@link PaymentGatewayService} and
 * every {@link PaymentGatewayStrategy} implementation without a circular/nested-class dependency.
 */
@Data
@Builder
public class PaymentGatewayResult {

    private boolean success;
    private String gatewayTransactionId;
    private String message;
    private String errorCode;
    private Map<String, Object> additionalData;

    public static PaymentGatewayResult success(String transactionId, String message) {
        return PaymentGatewayResult.builder()
                .success(true)
                .gatewayTransactionId(transactionId)
                .message(message)
                .build();
    }

    public static PaymentGatewayResult failure(String message) {
        return PaymentGatewayResult.builder().success(false).message(message).build();
    }

    public static PaymentGatewayResult failure(String message, String errorCode) {
        return PaymentGatewayResult.builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
