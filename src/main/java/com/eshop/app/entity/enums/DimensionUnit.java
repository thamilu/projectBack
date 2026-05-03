package com.eshop.app.entity.enums;

import java.math.BigDecimal;

/**
 * Dimension units.
 */
public enum DimensionUnit {
    CM("Centimeter", BigDecimal.ONE),
    M("Meter", new BigDecimal("100")),
    IN("Inch", new BigDecimal("2.54")),
    FT("Foot", new BigDecimal("30.48"));

    private final String displayName;
    private final BigDecimal toCmMultiplier;

    DimensionUnit(String displayName, BigDecimal toCmMultiplier) {
        this.displayName = displayName;
        this.toCmMultiplier = toCmMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal convertToCm(BigDecimal value) {
        return value.multiply(toCmMultiplier);
    }
}
