package com.eshop.app.enums;

public enum SellerIdentityType {
    INDIVIDUAL("Individual Seller", "Selling as an independent person. Requires PAN and Aadhaar."),
    BUSINESS("Registered Business", "Selling as a company (LLP, Pvt Ltd, etc). Requires GSTIN.");

    private final String displayName;
    private final String description;

    SellerIdentityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
