package com.eshop.app.payment.infrastructure.gateway;

import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.application.service.PaymentGatewayResult;
import com.eshop.app.payment.application.service.PaymentGatewayStrategy;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.model.PaymentGateway;
import com.eshop.app.payment.infrastructure.config.PaymentProperties;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Razorpay integration.
 *
 * <p>Razorpay's checkout flow completes client-side; the backend never collects card/UPI
 * credentials. This strategy only ever receives the resulting
 * {@code razorpayOrderId}/{@code razorpayPaymentId}/{@code razorpaySignature} triple, verifies it
 * via the official SDK ({@link Utils#verifyPaymentSignature}), then confirms the payment's real
 * status via the Razorpay API rather than trusting the client-reported outcome.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RazorpayPaymentGatewayStrategy implements PaymentGatewayStrategy {

    private static final String STATUS_CAPTURED = "captured";

    private final PaymentProperties paymentProperties;

    @Override
    public PaymentGateway getGateway() {
        return PaymentGateway.RAZORPAY;
    }

    @Override
    public boolean isEnabled() {
        PaymentProperties.RazorpayConfig config = paymentProperties.getRazorpay();
        return config.isEnabled()
                && StringUtils.hasText(config.getKeyId())
                && StringUtils.hasText(config.getKeySecret());
    }

    @Override
    public PaymentGatewayResult process(PaymentRequest request, Payment payment) {
        String orderId = request.getRazorpayOrderId();
        String paymentId = request.getRazorpayPaymentId();
        String signature = request.getRazorpaySignature();

        if (!StringUtils.hasText(orderId)
                || !StringUtils.hasText(paymentId)
                || !StringUtils.hasText(signature)) {
            return PaymentGatewayResult.failure(
                    "razorpayOrderId, razorpayPaymentId and razorpaySignature are all required",
                    "MISSING_RAZORPAY_FIELDS");
        }

        String keySecret = paymentProperties.getRazorpay().getKeySecret();
        JSONObject signaturePayload =
                new JSONObject()
                        .put("razorpay_order_id", orderId)
                        .put("razorpay_payment_id", paymentId)
                        .put("razorpay_signature", signature);
        try {
            if (!Utils.verifyPaymentSignature(signaturePayload, keySecret)) {
                log.warn("Razorpay signature verification failed for order {}", request.getOrderId());
                return PaymentGatewayResult.failure(
                        "Invalid Razorpay payment signature", "INVALID_SIGNATURE");
            }
        } catch (RazorpayException e) {
            log.warn("Razorpay signature verification error for order {}", request.getOrderId(), e);
            return PaymentGatewayResult.failure("Invalid Razorpay payment signature", "INVALID_SIGNATURE");
        }

        try {
            RazorpayClient client = newClient();
            JSONObject remotePayment = client.payments.fetch(paymentId).toJson();
            String remoteStatus = remotePayment.optString("status");

            if (!STATUS_CAPTURED.equals(remoteStatus)) {
                log.warn(
                        "Razorpay payment {} signature valid but not captured: status={}",
                        paymentId,
                        remoteStatus);
                return PaymentGatewayResult.failure(
                        "Razorpay payment not captured: " + remoteStatus, "NOT_CAPTURED");
            }

            long expectedPaise = toSmallestUnit(request.getAmount());
            long remotePaise = remotePayment.optLong("amount", -1);
            if (remotePaise != expectedPaise) {
                log.error(
                        "Razorpay amount mismatch for payment {}: expected={} actual={}",
                        paymentId,
                        expectedPaise,
                        remotePaise);
                return PaymentGatewayResult.failure(
                        "Razorpay captured amount does not match order amount", "AMOUNT_MISMATCH");
            }

            payment.setUpiReferenceId(remotePayment.optString("acquirer_data", null));
            return PaymentGatewayResult.success(paymentId, "Payment successful via Razorpay");

        } catch (RazorpayException e) {
            log.error("Razorpay API call failed for payment {}", paymentId, e);
            return PaymentGatewayResult.failure("Razorpay verification failed: " + e.getMessage());
        }
    }

    @Override
    public PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason) {
        if (!StringUtils.hasText(payment.getGatewayTransactionId())) {
            return PaymentGatewayResult.failure(
                    "Payment has no Razorpay payment id to refund", "MISSING_GATEWAY_REFERENCE");
        }
        try {
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", toSmallestUnit(amount));
            if (StringUtils.hasText(reason)) {
                refundRequest.put("notes", new JSONObject().put("reason", reason));
            }

            RazorpayClient client = newClient();
            JSONObject refund =
                    client.payments.refund(payment.getGatewayTransactionId(), refundRequest).toJson();

            return PaymentGatewayResult.success(
                    refund.optString("id"), "Refund processed via Razorpay");
        } catch (RazorpayException e) {
            log.error("Razorpay refund failed for payment {}", payment.getId(), e);
            return PaymentGatewayResult.failure("Razorpay refund failed: " + e.getMessage());
        }
    }

    private long toSmallestUnit(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    private RazorpayClient newClient() throws RazorpayException {
        return new RazorpayClient(
                paymentProperties.getRazorpay().getKeyId(), paymentProperties.getRazorpay().getKeySecret());
    }
}
