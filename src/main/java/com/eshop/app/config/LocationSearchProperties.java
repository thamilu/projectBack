package com.eshop.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "location.search")
public class LocationSearchProperties {
    private double defaultRadiusKm = 10.0;
    private double maxRadiusKm = 500.0;
    private int defaultPageSize = 20;

    public double getDefaultRadiusKm() { return defaultRadiusKm; }
    public void setDefaultRadiusKm(double defaultRadiusKm) { this.defaultRadiusKm = defaultRadiusKm; }
    public double getMaxRadiusKm() { return maxRadiusKm; }
    public void setMaxRadiusKm(double maxRadiusKm) { this.maxRadiusKm = maxRadiusKm; }
    public int getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }
}
