package com.eshop.app.entity.enums;

import java.math.BigDecimal;

/**
 * Weight units.
 */
public enum WeightUnit {
    KG("Kilogram", BigDecimal.ONE),
    G("Gram", new BigDecimal("0.001")),
    LB("Pound", new BigDecimal("0.453592")),
    OZ("Ounce", new BigDecimal("0.0283495"));

    private final String displayName;
    private final BigDecimal toKgMultiplier;

    WeightUnit(String displayName, BigDecimal toKgMultiplier) {
        this.displayName = displayName;
        this.toKgMultiplier = toKgMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal convertToKg(BigDecimal value) {
        return value.multiply(toKgMultiplier);
    }
}
