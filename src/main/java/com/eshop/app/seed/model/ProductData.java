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
        List<String> tags,
        /**
         * Whether the homepage's "Featured Products" section (which filters
         * on featured=true) should be able to show this product. Nullable —
         * absent in JSON means "not featured", same as the previous
         * hardcoded default in ProductSeeder. Previously there was no way
         * to seed a featured product at all, so that section always fell
         * back to 100% demo/placeholder data regardless of catalog size.
         */
        Boolean featured,
        /**
         * Image URLs for this product, in display order; the first becomes
         * the primary image (see ProductSeeder#buildProduct). May be a
         * relative frontend-static path (e.g. "/images/products/x.png") or
         * an absolute URL on an allow-listed image host. Nullable/absent —
         * a product with no images renders the frontend's own "image coming
         * soon" empty state, which is correct when a real product genuinely
         * has no photo yet.
         */
        List<String> imageUrls) {
    public static ProductData of(String name, String sku, double price, double discountPrice,
            String categoryName, String brandName, String storeName, String... tags) {
        return new ProductData(name, sku, price, discountPrice, null, categoryName, brandName, storeName,
                List.of(tags), null, null);
    }
}
