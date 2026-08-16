package com.eshop.app.payment.application.service.impl;

import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.api.response.PaymentResponse;
import com.eshop.app.payment.application.mapper.PaymentMapper;
import com.eshop.app.payment.application.port.in.ProcessPaymentUseCase;
import com.eshop.app.payment.application.service.PaymentGatewayResult;
import com.eshop.app.payment.application.service.PaymentGatewayService;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.repository.PaymentRepository;
import com.eshop.app.payment.domain.model.PaymentGateway;
import com.eshop.app.payment.domain.model.PaymentStatus;
import com.eshop.app.payment.infrastructure.config.PaymentProperties;
import com.eshop.app.payment.infrastructure.security.PaymentSignatureVerifier;
import com.eshop.app.payment.shared.exception.PaymentException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class ProcessPaymentUseCaseImpl implements ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentMapper paymentMapper;
    private final AppProperties appProperties;
    private final PaymentProperties paymentProperties;
    private final PaymentGatewayService paymentGatewayService;
    private final PaymentSignatureVerifier signatureVerifier;
    private final ObjectMapper objectMapper;

    @Value("${payment.payu.salt:}")
    private String payuSalt;

    @Value("${payment.payu.key:}")
    private String payuKey;

    @Value("${payment.cashfree.webhook-secret:}")
    private String cashfreeWebhookSecret;

    @Value("${payment.upi.webhook-secret:}")
    private String upiWebhookSecret;

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing payment for order: {}, gateway: {}", request.getOrderId(), request.getGateway());

        validatePaymentRequest(request);

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        validateOrderEligibility(order);

        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new PaymentException(String.format("Payment amount mismatch. Order: %s, Payment: %s",
                    order.getTotalAmount(), request.getAmount()));
        }

        Payment payment = Payment.builder()
                .order(order)
                .transactionId(generateTransactionId())
                .gateway(request.getGateway())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .build();

        payment = paymentRepository.save(payment);

        try {
            PaymentGatewayResult result = paymentGatewayService.processPayment(request, payment);

            if (result.isSuccess()) {
                payment.markAsProcessed(PaymentStatus.COMPLETED, result.getGatewayTransactionId());
                order.markAsPaid();
                orderRepository.save(order);
                log.info("Payment processed successfully: {}", payment.getTransactionId());
            } else {
                payment.markAsProcessed(PaymentStatus.FAILED, result.getGatewayTransactionId());
                payment.setFailureReason(result.getMessage());
                payment.setResponseCode(result.getErrorCode());
                log.warn("Payment failed: {}, reason: {}", payment.getTransactionId(), result.getMessage());
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment processing error: " + e.getMessage());
            log.error("Payment processing exception for transaction: {}", payment.getTransactionId(), e);
        }

        payment = paymentRepository.save(payment);
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse verifyPayment(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + transactionId));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse retryPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (payment.getStatus() != PaymentStatus.FAILED) {
            throw new PaymentException("Only failed payments can be retried");
        }

        PaymentRequest retryRequest = PaymentRequest.builder()
                .orderId(payment.getOrder().getId())
                .gateway(payment.getGateway())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .build();

        return processPayment(retryRequest);
    }

    @Override
    public PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        payment.setStatus(status);
        if (reason != null)
            payment.setFailureReason(reason);
        payment = paymentRepository.save(payment);
        return paymentMapper.toResponse(payment);
    }

    // Webhook Handlers Implementation

    @Override
    @SuppressWarnings("unchecked")
    public void handleStripeWebhook(String payload, String signature) {
        String stripeWebhookSecret = paymentProperties.getStripe().getWebhookSecret();
        if (!signatureVerifier.verifyStripeStyleSignature(payload, signature, stripeWebhookSecret)) {
            log.warn("Invalid Stripe webhook signature");
            throw new PaymentException("Invalid signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");
            Map<String, Object> eventData = (Map<String, Object>) event.get("data");
            Map<String, Object> object = (Map<String, Object>) eventData.get("object");

            String paymentIntentId = (String) object.get("id");
            log.info("Received Stripe webhook: {} for payment {}", eventType, paymentIntentId);

            switch (eventType) {
                case "payment_intent.succeeded" -> handlePaymentSuccess(paymentIntentId, "STRIPE", object);
                case "payment_intent.payment_failed" -> handlePaymentFailure(paymentIntentId, "STRIPE", object);
                case "payment_intent.requires_action" -> handlePaymentRequiresAction(paymentIntentId, "STRIPE", object);
                case "payment_intent.canceled" -> handlePaymentCanceled(paymentIntentId, "STRIPE", object);
                default -> log.info("Unhandled Stripe event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            throw new PaymentException("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleRazorpayWebhook(String payload, String signature) {
        String razorpayWebhookSecret = paymentProperties.getRazorpay().getWebhookSecret();
        if (!signatureVerifier.verifyHmacSha256Hex(payload, signature, razorpayWebhookSecret)) {
            log.warn("Invalid Razorpay webhook signature");
            throw new PaymentException("Invalid signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("event");
            Map<String, Object> payload_data = (Map<String, Object>) event.get("payload");
            Map<String, Object> payment = (Map<String, Object>) payload_data.get("payment");
            Map<String, Object> entity = payment != null ? payment : (Map<String, Object>) payload_data.get("order");

            String entityId = (String) entity.get("id");
            log.info("Received Razorpay webhook: {} for entity {}", eventType, entityId);

            switch (eventType) {
                case "payment.captured" -> handlePaymentSuccess(entityId, "RAZORPAY", entity);
                case "payment.failed" -> handlePaymentFailure(entityId, "RAZORPAY", entity);
                case "order.paid" -> handleOrderPaid(entityId, "RAZORPAY", entity);
                default -> log.info("Unhandled Razorpay event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            throw new PaymentException("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    public void handlePayUWebhook(String payload) {
        try {
            Map<String, String> params = parseFormData(payload);

            if (!verifyPayUHash(params)) {
                log.warn("Invalid PayU webhook hash for transaction {}", params.get("txnid"));
                throw new PaymentException("Invalid signature");
            }

            String status = params.get("status");
            String transactionId = params.get("txnid");

            log.info("Received PayU webhook: {} for transaction {}", status, transactionId);

            switch (status.toLowerCase()) {
                case "success" -> handlePaymentSuccess(transactionId, "PAYU", convertToMap(params));
                case "failure" -> handlePaymentFailure(transactionId, "PAYU", convertToMap(params));
                case "pending" -> handlePaymentPending(transactionId, "PAYU", convertToMap(params));
                default -> log.info("Unhandled PayU status: {}", status);
            }
        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing PayU webhook", e);
            throw new PaymentException("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleCashfreeWebhook(String payload, String signature) {
        if (!signatureVerifier.verifyHmacSha256Hex(payload, signature, cashfreeWebhookSecret)) {
            log.warn("Invalid Cashfree webhook signature");
            throw new PaymentException("Invalid signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");
            Map<String, Object> data = (Map<String, Object>) event.get("data");

            String orderId = (String) data.get("order_id");
            log.info("Received Cashfree webhook: {} for order {}", eventType, orderId);

            switch (eventType) {
                case "PAYMENT_SUCCESS_WEBHOOK" -> handlePaymentSuccess(orderId, "CASHFREE", data);
                case "PAYMENT_FAILED_WEBHOOK" -> handlePaymentFailure(orderId, "CASHFREE", data);
                case "PAYMENT_USER_DROPPED_WEBHOOK" -> handlePaymentCanceled(orderId, "CASHFREE", data);
                default -> log.info("Unhandled Cashfree event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing Cashfree webhook", e);
            throw new PaymentException("Error processing webhook: " + e.getMessage());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleUpiWebhook(String payload, String signature) {
        if (!signatureVerifier.verifyHmacSha256Hex(payload, signature, upiWebhookSecret)) {
            log.warn("Invalid UPI webhook signature");
            throw new PaymentException("Invalid signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String transactionId = (String) event.get("transactionId");
            String status = (String) event.get("status");

            log.info("Received UPI webhook: {} for transaction {}", status, transactionId);

            switch (status.toLowerCase()) {
                case "success" -> handleUpiPaymentSuccess(transactionId, event);
                case "failed" -> handleUpiPaymentFailure(transactionId, event);
                case "pending" -> handleUpiPaymentPending(transactionId, event);
                default -> log.info("Unhandled UPI status: {}", status);
            }
        } catch (Exception e) {
            log.error("Error processing UPI webhook", e);
            throw new PaymentException("Error processing webhook: " + e.getMessage());
        }
    }

    // Helper Webhook Persistence Methods

    private void handlePaymentSuccess(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setGatewayTransactionId((String) data.get("id"));
            payment.setResponseCode("SUCCESS");
            payment.setResponseMessage("Payment completed successfully");
            payment.setCompletedAt(LocalDateTime.now());

            extractPaymentDetails(payment, data, gateway);

            paymentRepository.save(payment);
            log.info("Payment {} marked as completed", payment.getId());
        } else {
            log.warn("Webhook success event for unknown payment reference: {} ({})", paymentRef, gateway);
        }
    }

    private void handlePaymentFailure(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode((String) data.get("failure_code"));
            payment.setResponseMessage((String) data.get("failure_reason"));
            payment.setFailedAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("Payment {} marked as failed", payment.getId());
        }
    }

    private void handlePaymentRequiresAction(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setResponseMessage("Payment requires additional authentication");

            paymentRepository.save(payment);
            log.info("Payment {} requires action", payment.getId());
        }
    }

    private void handlePaymentCanceled(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setResponseMessage("Payment canceled by user");
            payment.setCancelledAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("Payment {} canceled", payment.getId());
        }
    }

    private void handlePaymentPending(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setResponseMessage("Payment is pending");

            paymentRepository.save(payment);
            log.info("Payment {} is pending", payment.getId());
        }
    }

    private void handleOrderPaid(String orderId, String gateway, Map<String, Object> data) {
        handlePaymentSuccess(orderId, gateway, data);
    }

    private void handleUpiPaymentSuccess(String transactionId, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByUpiTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setUpiTransactionId((String) data.get("upiTransactionRef"));
            payment.setResponseCode("SUCCESS");
            payment.setResponseMessage("UPI payment successful");
            payment.setCompletedAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("UPI payment {} completed", payment.getId());
        }
    }

    private void handleUpiPaymentFailure(String transactionId, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByUpiTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setResponseCode((String) data.get("errorCode"));
            payment.setResponseMessage((String) data.get("errorMessage"));
            payment.setFailedAt(LocalDateTime.now());

            paymentRepository.save(payment);
            log.info("UPI payment {} failed", payment.getId());
        }
    }

    private void handleUpiPaymentPending(String transactionId, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByUpiTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setResponseMessage("UPI payment pending");

            paymentRepository.save(payment);
            log.info("UPI payment {} is pending", payment.getId());
        }
    }

    private Optional<Payment> findPaymentByReference(String reference, String gateway) {
        return paymentRepository.findByGatewayTransactionIdAndGateway(reference, PaymentGateway.valueOf(gateway));
    }

    private Optional<Payment> findPaymentByUpiTransactionId(String transactionId) {
        return paymentRepository.findByUpiTransactionId(transactionId);
    }

    @SuppressWarnings("unchecked")
    private void extractPaymentDetails(Payment payment, Map<String, Object> data, String gateway) {
        switch (gateway) {
            case "STRIPE" -> {
                Map<String, Object> charges = (Map<String, Object>) data.get("charges");
                if (charges != null && charges.get("data") instanceof List<?> chargesList && !chargesList.isEmpty()) {
                    Map<String, Object> charge = (Map<String, Object>) chargesList.get(0);
                    Map<String, Object> paymentMethod = (Map<String, Object>) charge.get("payment_method_details");
                    if (paymentMethod != null && paymentMethod.get("card") != null) {
                        Map<String, Object> card = (Map<String, Object>) paymentMethod.get("card");
                        payment.setCardLastFour((String) card.get("last4"));
                        payment.setCardBrand((String) card.get("brand"));
                    }
                }
            }
            case "RAZORPAY" -> {
                payment.setGatewayTransactionId((String) data.get("id"));
                if (data.get("card") != null) {
                    Map<String, Object> card = (Map<String, Object>) data.get("card");
                    payment.setCardLastFour((String) card.get("last4"));
                    payment.setCardBrand((String) card.get("network"));
                }
            }
            default -> { /* no gateway-specific card details to extract */ }
        }
    }

    // PayU reverse-hash (response/webhook) verification.
    //
    // PayU's documented scheme: sha512(salt|status|||||udf5|udf4|udf3|udf2|udf1|email|firstname|
    // productinfo|amount|txnid|key). Unused udf slots are empty strings regardless of how many
    // the merchant account actually uses. If this integration's PayU account is configured with a
    // non-standard field set, this must be updated to match the exact sequence in the PayU
    // merchant dashboard — a mismatch here fails ALL PayU webhooks closed (safe), never open.
    private boolean verifyPayUHash(Map<String, String> params) {
        String hash = params.get("hash");
        String key = params.getOrDefault("key", payuKey);
        StringBuilder sequence = new StringBuilder();
        sequence.append(payuSalt).append('|');
        sequence.append(params.getOrDefault("status", "")).append('|');
        sequence.append("|||||"); // reserved fields
        for (int i = 5; i >= 1; i--) {
            sequence.append(params.getOrDefault("udf" + i, "")).append('|');
        }
        sequence.append(params.getOrDefault("email", "")).append('|');
        sequence.append(params.getOrDefault("firstname", "")).append('|');
        sequence.append(params.getOrDefault("productinfo", "")).append('|');
        sequence.append(params.getOrDefault("amount", "")).append('|');
        sequence.append(params.getOrDefault("txnid", "")).append('|');
        sequence.append(key);

        return signatureVerifier.verifySha512Hex(sequence.toString(), hash, payuSalt);
    }

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private Map<String, Object> convertToMap(Map<String, String> stringMap) {
        return new HashMap<>(stringMap);
    }

    @SuppressWarnings("unused")
    private PaymentResponse processPaymentFallback(PaymentRequest request, Exception e) {
        log.error("Payment gateway fallback for order: {}", request.getOrderId());
        throw new PaymentException("Payment gateway is temporarily unavailable: " + e.getMessage());
    }

    private void validatePaymentRequest(PaymentRequest request) {
        AppProperties.Business business = appProperties.getBusiness();
        BigDecimal minAmount = BigDecimal.valueOf(business.getMinPaymentAmount());
        BigDecimal maxAmount = BigDecimal.valueOf(business.getMaxPaymentAmount());
        Set<String> allowedGateways = Set.copyOf(java.util.Arrays.asList(business.getAllowedGateways().toUpperCase().split(",")));

        if (request.getAmount() == null || request.getAmount().compareTo(minAmount) < 0
                || request.getAmount().compareTo(maxAmount) > 0) {
            throw new PaymentException("Invalid payment amount");
        }
        if (request.getGateway() == null || !allowedGateways.contains(request.getGateway().toString().toUpperCase())) {
            throw new PaymentException("Invalid payment gateway");
        }
    }

    private void validateOrderEligibility(Order order) {
        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED
                || order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new PaymentException("Order is not eligible for payment");
        }
    }

    private String generateTransactionId() {
        return "TXN_" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
