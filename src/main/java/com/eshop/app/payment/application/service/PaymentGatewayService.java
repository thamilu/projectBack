package com.eshop.app.payment.application.service;

import com.eshop.app.payment.api.request.PaymentRequest;
import com.eshop.app.payment.domain.entity.Payment;
import com.eshop.app.payment.domain.model.PaymentGateway;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Routes payment processing and refund requests to the {@link PaymentGatewayStrategy} registered
 * for the requested {@link PaymentGateway}.
 *
 * <p>Gateways without a registered strategy bean (not yet integrated) fail explicitly rather than
 * silently reporting success — this module has no gateway-specific logic of its own, keeping it
 * open for extension (new gateway = new {@link PaymentGatewayStrategy} bean) without modification.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentGatewayService {

    private final List<PaymentGatewayStrategy> strategies;

    private Map<PaymentGateway, PaymentGatewayStrategy> strategiesByGateway;

    @PostConstruct
    void init() {
        strategiesByGateway = new EnumMap<>(PaymentGateway.class);
        for (PaymentGatewayStrategy strategy : strategies) {
            strategiesByGateway.put(strategy.getGateway(), strategy);
        }
        log.info(
                "Payment gateway service initialized. Integrated gateways: {}",
                strategiesByGateway.keySet());
    }

    public PaymentGatewayResult processPayment(PaymentRequest request, Payment payment) {
        PaymentGatewayStrategy strategy = resolveStrategy(request.getGateway());
        if (strategy == null) {
            return PaymentGatewayResult.failure(
                    "Payment gateway not yet integrated: " + request.getGateway(),
                    "GATEWAY_NOT_INTEGRATED");
        }
        if (!strategy.isEnabled()) {
            return PaymentGatewayResult.failure(
                    "Payment gateway is not enabled: " + request.getGateway(),
                    "GATEWAY_DISABLED");
        }
        return strategy.process(request, payment);
    }

    public PaymentGatewayResult refund(Payment payment, BigDecimal amount, String reason) {
        PaymentGatewayStrategy strategy = resolveStrategy(payment.getGateway());
        if (strategy == null) {
            return PaymentGatewayResult.failure(
                    "Payment gateway not yet integrated: " + payment.getGateway(),
                    "GATEWAY_NOT_INTEGRATED");
        }
        if (!strategy.isEnabled()) {
            return PaymentGatewayResult.failure(
                    "Payment gateway is not enabled: " + payment.getGateway(), "GATEWAY_DISABLED");
        }
        return strategy.refund(payment, amount, reason);
    }

    private PaymentGatewayStrategy resolveStrategy(PaymentGateway gateway) {
        return strategiesByGateway.get(gateway);
    }
}
