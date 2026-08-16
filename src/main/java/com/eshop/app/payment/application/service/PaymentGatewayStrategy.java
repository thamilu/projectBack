package com.eshop.app.payment.application.service;

import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.model.PaymentGateway;
import java.math.BigDecimal;

/**
 * Strategy contract for a single payment gateway integration (Stripe, Razorpay, ...).
 *
 * <p>Implementations MUST NOT accept or transmit raw card data (PAN/CVV/expiry) to stay out of
 * PCI-DSS SAQ D scope — payment must be authorized using a gateway-issued token (e.g. Stripe
 * {@code payment_method} id, or a client-confirmed Razorpay payment + signature), never
 * {@link PaymentRequest#getCardInfo()}.
 *
 * <p>New gateways are added by registering a new Spring bean implementing this interface —
 * {@link PaymentGatewayService} discovers all strategies automatically (Open/Closed Principle).
 */
public interface PaymentGatewayStrategy {

    /** The gateway this strategy handles. */
    PaymentGateway getGateway();

    /** Whether this gateway is configured and enabled for use. */
    boolean isEnabled();

    /**
     * Authorizes/captures a payment using an already gateway-tokenized reference from the client
     * (never raw card data).
     */
    PaymentGatewayResult process(PaymentRequest request, Payment payment);

    /** Refunds a previously captured payment, in whole or in part. */
    PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason);
}
