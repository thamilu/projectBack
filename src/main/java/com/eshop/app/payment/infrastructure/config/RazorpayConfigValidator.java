package com.eshop.app.payment.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Startup validator for Razorpay configuration.
 *
 * <p>Ensures that when Razorpay is enabled, all required secrets are configured — fails fast at
 * startup rather than at runtime during payment processing.
 */
@Component
@ConditionalOnProperty(name = "payment.razorpay.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RazorpayConfigValidator implements InitializingBean {

    private final PaymentProperties paymentProperties;

    @Override
    public void afterPropertiesSet() {
        log.info("Validating Razorpay configuration...");
        PaymentProperties.RazorpayConfig config = paymentProperties.getRazorpay();

        Assert.hasText(config.getKeyId(),
                "payment.razorpay.key-id is required when payment.razorpay.enabled=true. "
                        + "Set RAZORPAY_KEY_ID environment variable.");

        Assert.hasText(config.getKeySecret(),
                "payment.razorpay.key-secret is required when payment.razorpay.enabled=true. "
                        + "Set RAZORPAY_KEY_SECRET environment variable.");

        Assert.hasText(config.getWebhookSecret(),
                "payment.razorpay.webhook-secret is required when payment.razorpay.enabled=true. "
                        + "Set RAZORPAY_WEBHOOK_SECRET environment variable.");

        log.info("Razorpay configuration validated successfully");
    }
}
