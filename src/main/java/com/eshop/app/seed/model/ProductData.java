package com.eshop.app.seed.model;

import java.util.List;

/**
 * Immutable record representing product seed data.
 */
public record ProductData(
        String name,
        String sku,
        double price,
        double discountPrice,
        String description,
        String categoryName,
        String brandName,
        String storeName,
        List<String> tags) {
    public static ProductData of(String name, String sku, double price, double discountPrice,
            String categoryName, String brandName, String storeName, String... tags) {
        return new ProductData(name, sku, price, discountPrice, null, categoryName, brandName, storeName,
                List.of(tags));
    }
}
