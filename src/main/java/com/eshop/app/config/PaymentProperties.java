package com.eshop.app.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "payment")
public class PaymentProperties {
    private boolean enabled = false;
    private RazorpayConfig razorpay = new RazorpayConfig();
    private StripeConfig stripe = new StripeConfig();
    private PaypalConfig paypal = new PaypalConfig();

    @Data
    public static class RazorpayConfig {
        private boolean enabled = false;
        private String keyId = "";
        private String keySecret = "";
        private String webhookSecret = "";
        private String currency = "INR";
    }

    @Data
    public static class StripeConfig {
        private boolean enabled = false;
        private String apiKey = "";
        private String publicKey = "";
        private String webhookSecret = "";
        private String currency = "USD";
    }

    @Data
    public static class PaypalConfig {
        private boolean enabled = false;
        private String clientId = "";
        private String clientSecret = "";
        private String mode = "sandbox";
    }
}
