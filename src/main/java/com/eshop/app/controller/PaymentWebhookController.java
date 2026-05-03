package com.eshop.app.controller;

import com.eshop.app.entity.Payment;
import com.eshop.app.repository.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Payment Webhook Controller
 * Handles real-time payment status updates from various payment gateways
 */
@Tag(name = "Payment Webhooks", description = "Payment gateway webhook handlers")
@RestController
@RequestMapping(ApiConstants.Endpoints.WEBHOOKS_PAYMENT)
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {
    
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${payment.webhook.stripe.secret:}")
    private String stripeWebhookSecret;
    
    @Value("${payment.webhook.razorpay.secret:}")
    private String razorpayWebhookSecret;
    
    @Value("${payment.webhook.payu.secret:}")
    private String payuWebhookSecret;
    
    @Value("${payment.webhook.cashfree.secret:}")
    private String cashfreeWebhookSecret;
    
    @PostMapping("/stripe")
    @Operation(summary = "Stripe Webhook", description = "Handle Stripe payment webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        try {
            // Verify webhook signature
            if (!verifyStripeSignature(payload, signature)) {
                log.warn("Invalid Stripe webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");
            Map<String, Object> eventData = (Map<String, Object>) event.get("data");
            Map<String, Object> object = (Map<String, Object>) eventData.get("object");
            
            String paymentIntentId = (String) object.get("id");
            // String status = (String) object.get("status"); // Available if needed
            
            log.info("Received Stripe webhook: {} for payment {}", eventType, paymentIntentId);
            
            switch (eventType) {
                case "payment_intent.succeeded" -> handlePaymentSuccess(paymentIntentId, "STRIPE", object);
                case "payment_intent.payment_failed" -> handlePaymentFailure(paymentIntentId, "STRIPE", object);
                case "payment_intent.requires_action" -> handlePaymentRequiresAction(paymentIntentId, "STRIPE", object);
                case "payment_intent.canceled" -> handlePaymentCanceled(paymentIntentId, "STRIPE", object);
                default -> log.info("Unhandled Stripe event type: {}", eventType);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
    
    @PostMapping("/razorpay")
    @Operation(summary = "Razorpay Webhook", description = "Handle Razorpay payment webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        
        try {
            // Verify webhook signature
            if (!verifyRazorpaySignature(payload, signature)) {
                log.warn("Invalid Razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("event");
            Map<String, Object> payload_data = (Map<String, Object>) event.get("payload");
            Map<String, Object> payment = (Map<String, Object>) payload_data.get("payment");
            Map<String, Object> entity = payment != null ? payment : (Map<String, Object>) payload_data.get("order");
            
            String entityId = (String) entity.get("id");
            // String status = (String) entity.get("status"); // Available if needed
            
            log.info("Received Razorpay webhook: {} for entity {}", eventType, entityId);
            
            switch (eventType) {
                case "payment.captured" -> handlePaymentSuccess(entityId, "RAZORPAY", entity);
                case "payment.failed" -> handlePaymentFailure(entityId, "RAZORPAY", entity);
                case "order.paid" -> handleOrderPaid(entityId, "RAZORPAY", entity);
                default -> log.info("Unhandled Razorpay event type: {}", eventType);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
    
    @PostMapping("/payu")
    @Operation(summary = "PayU Webhook", description = "Handle PayU payment webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handlePayUWebhook(@RequestBody String payload) {
        
        try {
            Map<String, String> params = parseFormData(payload);
            String status = params.get("status");
            String transactionId = params.get("txnid");
            // String payuId = params.get("mihpayid"); // Available if needed
            
            log.info("Received PayU webhook: {} for transaction {}", status, transactionId);
            
            switch (status.toLowerCase()) {
                case "success" -> handlePaymentSuccess(transactionId, "PAYU", convertToMap(params));
                case "failure" -> handlePaymentFailure(transactionId, "PAYU", convertToMap(params));
                case "pending" -> handlePaymentPending(transactionId, "PAYU", convertToMap(params));
                default -> log.info("Unhandled PayU status: {}", status);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing PayU webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
    
    @PostMapping("/cashfree")
    @Operation(summary = "Cashfree Webhook", description = "Handle Cashfree payment webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleCashfreeWebhook(
            @RequestBody String payload,
            @RequestHeader("x-webhook-signature") String signature) {
        
        try {
            // Verify webhook signature
            if (!verifyCashfreeSignature(payload, signature)) {
                log.warn("Invalid Cashfree webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
            }
            
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("type");
            Map<String, Object> data = (Map<String, Object>) event.get("data");
            
            String orderId = (String) data.get("order_id");
            // String paymentId = (String) data.get("cf_payment_id"); // Available if needed
            
            log.info("Received Cashfree webhook: {} for order {}", eventType, orderId);
            
            switch (eventType) {
                case "PAYMENT_SUCCESS_WEBHOOK" -> handlePaymentSuccess(orderId, "CASHFREE", data);
                case "PAYMENT_FAILED_WEBHOOK" -> handlePaymentFailure(orderId, "CASHFREE", data);
                case "PAYMENT_USER_DROPPED_WEBHOOK" -> handlePaymentCanceled(orderId, "CASHFREE", data);
                default -> log.info("Unhandled Cashfree event type: {}", eventType);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing Cashfree webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
    
    @PostMapping("/upi")
    @Operation(summary = "UPI Webhook", description = "Handle UPI payment webhook events")
    @ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    @SuppressWarnings("unchecked")
    public ResponseEntity<String> handleUpiWebhook(@RequestBody String payload) {
        
        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String transactionId = (String) event.get("transactionId");
            String status = (String) event.get("status");
            // String upiRef = (String) event.get("upiTransactionRef"); // Available if needed
            
            log.info("Received UPI webhook: {} for transaction {}", status, transactionId);
            
            switch (status.toLowerCase()) {
                case "success" -> handleUpiPaymentSuccess(transactionId, event);
                case "failed" -> handleUpiPaymentFailure(transactionId, event);
                case "pending" -> handleUpiPaymentPending(transactionId, event);
                default -> log.info("Unhandled UPI status: {}", status);
            }
            
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            log.error("Error processing UPI webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }
    
    // Helper Methods
    
    private void handlePaymentSuccess(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setGatewayTransactionId((String) data.get("id"));
            payment.setResponseCode("SUCCESS");
            payment.setResponseMessage("Payment completed successfully");
            payment.setCompletedAt(LocalDateTime.now());
            
            // Extract additional payment details
            extractPaymentDetails(payment, data, gateway);
            
            paymentRepository.save(payment);
            log.info("Payment {} marked as completed", payment.getId());
        }
    }
    
    private void handlePaymentFailure(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.FAILED);
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
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
            payment.setResponseMessage("Payment requires additional authentication");
            
            paymentRepository.save(payment);
            log.info("Payment {} requires action", payment.getId());
        }
    }
    
    private void handlePaymentCanceled(String paymentRef, String gateway, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByReference(paymentRef, gateway);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
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
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setResponseMessage("Payment is pending");
            
            paymentRepository.save(payment);
            log.info("Payment {} is pending", payment.getId());
        }
    }
    
    private void handleOrderPaid(String orderId, String gateway, Map<String, Object> data) {
        // Handle order-level payment completion
        handlePaymentSuccess(orderId, gateway, data);
    }
    
    private void handleUpiPaymentSuccess(String transactionId, Map<String, Object> data) {
        Optional<Payment> paymentOpt = findPaymentByUpiTransactionId(transactionId);
        if (paymentOpt.isPresent()) {
            Payment payment = paymentOpt.get();
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
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
            payment.setStatus(Payment.PaymentStatus.FAILED);
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
            payment.setStatus(Payment.PaymentStatus.PENDING);
            payment.setResponseMessage("UPI payment pending");
            
            paymentRepository.save(payment);
            log.info("UPI payment {} is pending", payment.getId());
        }
    }
    
    private Optional<Payment> findPaymentByReference(String reference, String gateway) {
        return paymentRepository.findByGatewayTransactionIdAndGateway(reference, 
            Payment.PaymentGateway.valueOf(gateway));
    }
    
    private Optional<Payment> findPaymentByUpiTransactionId(String transactionId) {
        return paymentRepository.findByUpiTransactionId(transactionId);
    }
    
    @SuppressWarnings("unchecked")
    private void extractPaymentDetails(Payment payment, Map<String, Object> data, String gateway) {
        switch (gateway) {
            case "STRIPE" -> {
                Map<String, Object> charges = (Map<String, Object>) data.get("charges");
                if (charges != null && charges.get("data") instanceof java.util.List) {
                    java.util.List<Map<String, Object>> chargesList = 
                        (java.util.List<Map<String, Object>>) charges.get("data");
                    if (!chargesList.isEmpty()) {
                        Map<String, Object> charge = chargesList.get(0);
                        Map<String, Object> paymentMethod = (Map<String, Object>) charge.get("payment_method_details");
                        if (paymentMethod != null && paymentMethod.get("card") != null) {
                            Map<String, Object> card = (Map<String, Object>) paymentMethod.get("card");
                            payment.setCardLastFour((String) card.get("last4"));
                            payment.setCardBrand((String) card.get("brand"));
                        }
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
        }
    }
    
    private boolean verifyStripeSignature(String payload, String signature) {
        if (stripeWebhookSecret.isEmpty()) return true; // Skip verification if secret not configured
        
        try {
            String[] signatureParts = signature.split(",");
            String timestamp = null;
            String v1Signature = null;
            
            for (String part : signatureParts) {
                if (part.startsWith("t=")) {
                    timestamp = part.substring(2);
                } else if (part.startsWith("v1=")) {
                    v1Signature = part.substring(3);
                }
            }
            
            String signedPayload = timestamp + "." + payload;
            String expectedSignature = computeHmacSha256(signedPayload, stripeWebhookSecret);
            
            return expectedSignature.equals(v1Signature);
        } catch (Exception e) {
            log.error("Error verifying Stripe signature", e);
            return false;
        }
    }
    
    private boolean verifyRazorpaySignature(String payload, String signature) {
        if (razorpayWebhookSecret.isEmpty()) return true;
        
        try {
            String expectedSignature = computeHmacSha256(payload, razorpayWebhookSecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying Razorpay signature", e);
            return false;
        }
    }
    
    private boolean verifyCashfreeSignature(String payload, String signature) {
        if (cashfreeWebhookSecret.isEmpty()) return true;
        
        try {
            String expectedSignature = computeHmacSha256(payload, cashfreeWebhookSecret);
            return expectedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying Cashfree signature", e);
            return false;
        }
    }
    
    private String computeHmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
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
}