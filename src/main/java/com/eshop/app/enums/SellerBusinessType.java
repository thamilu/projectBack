package com.eshop.app.enums;

public enum SellerBusinessType {
    FARMER("Farmer / Producer"),
    WHOLESALER("Wholesaler"),
    RETAILER("Retailer");

    private final String displayName;

    SellerBusinessType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
