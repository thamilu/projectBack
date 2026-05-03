package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "emi")
public class EmiProperties {
    private boolean enabled;
    private long minAmount;
    private int maxTenureMonths;
    private String supportedBanks;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getMinAmount() { return minAmount; }
    public void setMinAmount(long minAmount) { this.minAmount = minAmount; }
    public int getMaxTenureMonths() { return maxTenureMonths; }
    public void setMaxTenureMonths(int maxTenureMonths) { this.maxTenureMonths = maxTenureMonths; }
    public String getSupportedBanks() { return supportedBanks; }
    public void setSupportedBanks(String supportedBanks) { this.supportedBanks = supportedBanks; }
}
