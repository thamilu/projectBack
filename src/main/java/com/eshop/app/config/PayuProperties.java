package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payu")
public class PayuProperties {
    private String merchantKey;
    private String merchantSalt;
    private String baseUrl;

    public String getMerchantKey() { return merchantKey; }
    public void setMerchantKey(String merchantKey) { this.merchantKey = merchantKey; }
    public String getMerchantSalt() { return merchantSalt; }
    public void setMerchantSalt(String merchantSalt) { this.merchantSalt = merchantSalt; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
