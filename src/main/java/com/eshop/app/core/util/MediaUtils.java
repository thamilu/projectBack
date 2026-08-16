package com.eshop.app.core.util;

import com.eshop.app.catalog.domain.entity.Product;
import org.hibernate.Hibernate;

/** Shared utility class for media-related operations, such as extracting the primary image URL. */
public final class MediaUtils {

    private MediaUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Resolves the primary image URL for a given Product, falling back to custom seller override
     * images or master catalog images if a direct primary image is not present.
     *
     * @param product the product
     * @return the primary image URL, or null if not found
     */
    public static String getPrimaryImageUrl(Product product) {
        if (product == null) {
            return null;
        }

        // 1. Use primary image if available
        if (product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getUrl();
        }

        // 2. Try custom seller override images
        try {
            if (product.getImages() != null
                    && Hibernate.isInitialized(product.getImages())
                    && !product.getImages().isEmpty()) {
                com.eshop.app.catalog.domain.entity.ProductImage img =
                        product.getImages().get(0);
                if (img != null && img.getUrl() != null) {
                    return img.getUrl();
                }
            }
        } catch (Exception e) {
            // Swallow and fall through
        }

        // 3. Fall back to master catalog images
        try {
            if (product.getMasterProduct() != null
                    && product.getMasterProduct().getImages() != null
                    && Hibernate.isInitialized(product.getMasterProduct().getImages())
                    && !product.getMasterProduct().getImages().isEmpty()) {
                com.eshop.app.catalog.domain.entity.ProductImage img =
                        product.getMasterProduct().getImages().get(0);
                if (img != null && img.getUrl() != null) {
                    return img.getUrl();
                }
            }
        } catch (Exception e) {
            // Swallow
        }

        return null;
    }

    /**
     * Resolves the primary image URL from a list of ProductMedia objects.
     *
     * @param media the list of product media
     * @return the primary image URL, or null if none or empty
     */
    public static String extractPrimaryImageUrl(
            java.util.List<com.eshop.app.catalog.domain.entity.ProductMedia> media) {
        if (media == null || media.isEmpty()) {
            return null;
        }
        return media.stream()
                .filter(m -> m != null && m.getIsPrimary() != null && m.getIsPrimary())
                .map(m -> m.getMediaUrl())
                .findFirst()
                .orElse(media.get(0).getMediaUrl());
    }
}
