package com.eshop.app.payment.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.payment.application.port.in.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Webhook Controller
 * Handles real-time payment status updates from various payment gateways by delegating
 * to the Use Case.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Payment Webhooks", description = "Payment gateway webhook handlers")
@RestController
@RequestMapping(ApiConstants.Endpoints.WEBHOOKS_PAYMENT)
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final ProcessPaymentUseCase processPaymentUseCase;

    @PostMapping("/stripe")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        log.info("Received Stripe webhook signature");
        processPaymentUseCase.handleStripeWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/razorpay")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String signature) {
        log.info("Received Razorpay webhook signature");
        processPaymentUseCase.handleRazorpayWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/payu")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handlePayUWebhook(@RequestBody String payload) {
        log.info("Received PayU webhook");
        processPaymentUseCase.handlePayUWebhook(payload);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/cashfree")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handleCashfreeWebhook(
            @RequestBody String payload,
            @RequestHeader("x-webhook-signature") String signature) {
        log.info("Received Cashfree webhook signature");
        processPaymentUseCase.handleCashfreeWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/upi")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Webhook processed successfully")
    public ResponseEntity<String> handleUpiWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Signature", required = false) String signature) {
        log.info("Received UPI webhook");
        processPaymentUseCase.handleUpiWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }
}
