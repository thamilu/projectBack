package com.eshop.app.seed.model;

/**
 * Immutable record representing store (shop) seed data.
 */
public record StoreData(
    String storeName,
    String description,
    String address,
    String city,
    String state,
    String pincode,
    String country,
    String phone,
    String email,
    String logoUrl,
    String sellerEmail,
    String sellerType
) {
    public static StoreData of(String storeName, String sellerEmail, String description) {
        return new StoreData(storeName, description, null, null, null, null, null, null, null, null, sellerEmail, "BUSINESS");
    }
    
    public static StoreData full(String storeName, String sellerEmail, String description, String sellerType) {
        return new StoreData(storeName, description, null, null, null, null, null, null, null, null, sellerEmail, sellerType);
    }
}
