package com.eshop.app.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * CRITICAL-004 FIX: Startup validator for payment gateway configuration.
 * 
 * <p>Ensures that when payment gateways are enabled, all required secrets are configured.
 * Fails fast at startup rather than at runtime during payment processing.
 * 
 * @author E-Shop Team
 * @since 2.0.1
 */
@Component
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class StripeConfigValidator implements InitializingBean {
    
    private final StripeProperties stripeProperties;
    
    @Override
    public void afterPropertiesSet() {
        log.info("Validating Stripe configuration...");
        
        Assert.hasText(stripeProperties.getSecretKey(), 
            "stripe.secret-key is required when stripe.enabled=true. " +
            "Set STRIPE_SECRET_KEY environment variable.");
        
        Assert.hasText(stripeProperties.getPublicKey(), 
            "stripe.public-key is required when stripe.enabled=true. " +
            "Set STRIPE_PUBLIC_KEY environment variable.");
        
        Assert.hasText(stripeProperties.getWebhookSecret(), 
            "stripe.webhook-secret is required when stripe.enabled=true. " +
            "Set STRIPE_WEBHOOK_SECRET environment variable.");
        
        // Validate key format (basic check)
        if (!stripeProperties.getSecretKey().startsWith("sk_")) {
            throw new IllegalStateException(
                "Invalid Stripe secret key format. Must start with 'sk_'. " +
                "Check STRIPE_SECRET_KEY configuration."
            );
        }
        
        if (!stripeProperties.getPublicKey().startsWith("pk_")) {
            throw new IllegalStateException(
                "Invalid Stripe public key format. Must start with 'pk_'. " +
                "Check STRIPE_PUBLIC_KEY configuration."
            );
        }
        
        log.info("Stripe configuration validated successfully");
    }
}
