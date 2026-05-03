package com.eshop.app.seed.model;

/**
 * Immutable record representing brand seed data.
 */
public record BrandData(
    String name,
    String description,
    String logoUrl
) {
    public static BrandData of(String name, String description, String logoUrl) {
        return new BrandData(name, description, logoUrl);
    }
}
