package com.eshop.app.payment.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Startup validator for Stripe configuration.
 *
 * <p>Ensures that when Stripe is enabled, all required secrets are configured — fails fast at
 * startup rather than at runtime during payment processing.
 */
@Component
@ConditionalOnProperty(name = "payment.stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StripeConfigValidator implements InitializingBean {

    private final PaymentProperties paymentProperties;

    @Override
    public void afterPropertiesSet() {
        log.info("Validating Stripe configuration...");
        PaymentProperties.StripeConfig config = paymentProperties.getStripe();

        Assert.hasText(config.getApiKey(),
                "payment.stripe.api-key is required when payment.stripe.enabled=true. "
                        + "Set STRIPE_SECRET_KEY environment variable.");

        Assert.hasText(config.getPublicKey(),
                "payment.stripe.public-key is required when payment.stripe.enabled=true. "
                        + "Set STRIPE_PUBLIC_KEY environment variable.");

        Assert.hasText(config.getWebhookSecret(),
                "payment.stripe.webhook-secret is required when payment.stripe.enabled=true. "
                        + "Set STRIPE_WEBHOOK_SECRET environment variable.");

        if (!config.getApiKey().startsWith("sk_")) {
            throw new IllegalStateException(
                    "Invalid Stripe secret key format. Must start with 'sk_'. "
                            + "Check STRIPE_SECRET_KEY configuration.");
        }

        if (!config.getPublicKey().startsWith("pk_")) {
            throw new IllegalStateException(
                    "Invalid Stripe public key format. Must start with 'pk_'. "
                            + "Check STRIPE_PUBLIC_KEY configuration.");
        }

        log.info("Stripe configuration validated successfully");
    }
}
