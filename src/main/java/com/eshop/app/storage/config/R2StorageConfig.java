package com.eshop.app.storage.config;

import org.springframework.util.Assert;

/**
 * Immutable, validated configuration record for Cloudflare R2 storage.
 *
 * <p>Constructed once at startup. Public URL is automatically normalized to always include a
 * trailing slash.
 *
 * <p>Provides URL-building helpers to avoid string concatenation at call sites.
 */
public record R2StorageConfig(
        String bucketName, String publicUrl, String accountId, String region) {

    /** Compact constructor — validates and normalizes on construction. */
    public R2StorageConfig {
        Assert.hasText(bucketName, "[R2StorageConfig] bucketName must not be blank");
        Assert.hasText(publicUrl, "[R2StorageConfig] publicUrl must not be blank");
        publicUrl = publicUrl.endsWith("/") ? publicUrl : publicUrl + "/";
    }

    /**
     * Builds a fully qualified public URL for a given storage key.
     *
     * @param key the R2 object key (e.g. "products/abc123.jpg")
     * @return full public URL
     */
    public String buildUrl(String key) {
        return publicUrl + key;
    }
}
