package com.eshop.app.media.shared.constants;

import java.time.Duration;
import java.util.Set;

/**
 * Media module constants.
 *
 * <p>Centralized constants for file validation, image sizes, and upload configuration. Prevents
 * magic strings/numbers throughout the media module.
 */
public final class MediaConstants {

    private MediaConstants() {
        throw new UnsupportedOperationException("Cannot instantiate constants class");
    }

    // ==================== FILE VALIDATION ====================

    /** Maximum file size: 5MB. */
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    /** Allowed MIME types for image uploads. */
    public static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp");

    /** Allowed file extensions (without dot). */
    public static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    // ==================== IMAGE SIZES ====================

    /** Thumbnail dimensions (product listing grid). */
    public static final int THUMBNAIL_SIZE = 300;

    /** Product card dimensions (category pages). */
    public static final int PRODUCT_CARD_SIZE = 600;

    /** Product main image dimensions (product detail page). */
    public static final int PRODUCT_MAIN_SIZE = 1200;

    /** Zoom image dimensions (hover zoom on product page). */
    public static final int ZOOM_SIZE = 1800;

    // ==================== UPLOAD CONFIGURATION ====================

    /** Default presigned URL expiry duration. */
    public static final Duration DEFAULT_PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15);

    /** Maximum images per product. */
    public static final int MAX_IMAGES_PER_PRODUCT = 10;

    /** Orphan cleanup threshold: assets older than this are cleaned up. */
    public static final Duration ORPHAN_CLEANUP_THRESHOLD = Duration.ofHours(24);

    // ==================== STORAGE PATHS ====================

    /** R2 storage folder prefix for product images. */
    public static final String PRODUCT_IMAGE_FOLDER = "products";

    /** R2 storage folder prefix for thumbnails. */
    public static final String THUMBNAIL_PREFIX = "thumb_";

    // ==================== CACHE ====================

    /** Cache key prefix for product media lists. */
    public static final String CACHE_KEY_PREFIX = "media:product:";

    /** Cache TTL for product media. */
    public static final Duration CACHE_TTL = Duration.ofHours(1);

    // ==================== FORMATS ====================

    /** Primary output format for optimized images. */
    public static final String PRIMARY_FORMAT = "webp";

    /** Fallback format for transparency support. */
    public static final String TRANSPARENCY_FORMAT = "png";

    /** Compression quality for WebP output (0.0 - 1.0). */
    public static final float WEBP_QUALITY = 0.85f;
}
