package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "upi")
public class UpiProperties {
    private boolean enabled;
    private int collectExpiryMinutes;
    private String supportedApps;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getCollectExpiryMinutes() { return collectExpiryMinutes; }
    public void setCollectExpiryMinutes(int collectExpiryMinutes) { this.collectExpiryMinutes = collectExpiryMinutes; }
    public String getSupportedApps() { return supportedApps; }
    public void setSupportedApps(String supportedApps) { this.supportedApps = supportedApps; }
}
