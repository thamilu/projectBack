package com.eshop.app.seed.model;

/**
 * Immutable record representing store (shop) seed data.
 */
public record StoreData(
    String storeName,
    String description,
    String address,
    String phone,
    String email,
    String logoUrl,
    String sellerUsername,
    String sellerType
) {
    public static StoreData of(String storeName, String sellerUsername, String description) {
        return new StoreData(storeName, description, null, null, null, null, sellerUsername, "BUSINESS");
    }
    
    public static StoreData full(String storeName, String sellerUsername, String description, String sellerType) {
        return new StoreData(storeName, description, null, null, null, null, sellerUsername, sellerType);
    }
}
