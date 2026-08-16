package com.eshop.app.payment.infrastructure.gateway;

import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.application.service.PaymentGatewayResult;
import com.eshop.app.payment.application.service.PaymentGatewayStrategy;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.model.PaymentGateway;
import com.eshop.app.payment.infrastructure.config.PaymentProperties;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Stripe integration using PaymentIntents.
 *
 * <p>Only ever authorizes payments via a client-tokenized {@code payment_method} id
 * ({@link PaymentRequest#getStripePaymentMethodId()}) created client-side via Stripe.js/Elements —
 * raw card numbers/CVVs are never accepted or transmitted by this service, keeping the backend out
 * of PCI-DSS SAQ D scope.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StripePaymentGatewayStrategy implements PaymentGatewayStrategy {

    private static final String STATUS_SUCCEEDED = "succeeded";

    private final PaymentProperties paymentProperties;

    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.STRIPE;
    }

    @Override
    public boolean isEnabled() {
        PaymentProperties.StripeConfig config = paymentProperties.getStripe();
        return config.isEnabled() && StringUtils.hasText(config.getApiKey());
    }

    @Override
    public PaymentGatewayResult process(PaymentRequest request, Payment payment) {
        if (!StringUtils.hasText(request.getStripePaymentMethodId())) {
            return PaymentGatewayResult.failure(
                    "stripePaymentMethodId is required for Stripe payments", "MISSING_PAYMENT_METHOD");
        }

        try {
            PaymentIntentCreateParams params =
                    PaymentIntentCreateParams.builder()
                            .setAmount(toSmallestUnit(request.getAmount()))
                            .setCurrency(request.getCurrency().toLowerCase())
                            .setPaymentMethod(request.getStripePaymentMethodId())
                            .setConfirm(true)
                            .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                            .setDescription(request.getDescription())
                            .putMetadata("orderId", String.valueOf(request.getOrderId()))
                            .putMetadata("transactionId", payment.getTransactionId())
                            .setAutomaticPaymentMethods(
                                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                            .setEnabled(true)
                                            .setAllowRedirects(
                                                    PaymentIntentCreateParams.AutomaticPaymentMethods
                                                            .AllowRedirects.NEVER)
                                            .build())
                            .build();

            PaymentIntent intent = PaymentIntent.create(params, requestOptions());

            if (STATUS_SUCCEEDED.equals(intent.getStatus())) {
                populateCardDetails(payment, intent);
                return PaymentGatewayResult.success(intent.getId(), "Payment successful via Stripe");
            }

            log.warn(
                    "Stripe PaymentIntent {} did not succeed synchronously: status={}",
                    intent.getId(),
                    intent.getStatus());
            return PaymentGatewayResult.builder()
                    .success(false)
                    .gatewayTransactionId(intent.getId())
                    .message("Stripe payment requires further action: " + intent.getStatus())
                    .errorCode("STRIPE_" + intent.getStatus().toUpperCase())
                    .additionalData(Map.of("clientSecret", intent.getClientSecret()))
                    .build();

        } catch (StripeException e) {
            log.error("Stripe payment failed for order {}", request.getOrderId(), e);
            return PaymentGatewayResult.failure(
                    "Stripe payment failed: " + e.getMessage(), e.getCode());
        }
    }

    @Override
    public PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason) {
        if (!StringUtils.hasText(payment.getGatewayTransactionId())) {
            return PaymentGatewayResult.failure(
                    "Payment has no Stripe PaymentIntent id to refund", "MISSING_GATEWAY_REFERENCE");
        }
        try {
            RefundCreateParams params =
                    RefundCreateParams.builder()
                            .setPaymentIntent(payment.getGatewayTransactionId())
                            .setAmount(toSmallestUnit(amount))
                            .setReason(mapRefundReason(reason))
                            .build();

            Refund refund = Refund.create(params, requestOptions());

            boolean success = STATUS_SUCCEEDED.equals(refund.getStatus()) || "pending".equals(refund.getStatus());
            return success
                    ? PaymentGatewayResult.success(refund.getId(), "Refund processed via Stripe")
                    : PaymentGatewayResult.failure("Stripe refund status: " + refund.getStatus());
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment {}", payment.getId(), e);
            return PaymentGatewayResult.failure("Stripe refund failed: " + e.getMessage(), e.getCode());
        }
    }

    private RefundCreateParams.Reason mapRefundReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        }
        return switch (reason.toUpperCase()) {
            case "FRAUDULENT" -> RefundCreateParams.Reason.FRAUDULENT;
            case "DUPLICATE" -> RefundCreateParams.Reason.DUPLICATE;
            default -> RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER;
        };
    }

    private void populateCardDetails(Payment payment, PaymentIntent intent) {
        if (intent.getPaymentMethod() == null) {
            return;
        }
        // Card brand/last4 are populated from the expanded PaymentMethod when available; the
        // synchronous create() response here does not expand it by default, so this is a no-op
        // unless a future change adds .addExpand("payment_method") to the create params.
    }

    private long toSmallestUnit(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(paymentProperties.getStripe().getApiKey()).build();
    }
}
