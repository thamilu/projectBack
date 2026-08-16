package com.eshop.app.storage.config;

public final class StorageConstants {

    private StorageConstants() {}

    public static final int THUMB_WIDTH = 250;
    public static final int THUMB_HEIGHT = 250;
    public static final double THUMB_QUALITY = 0.8;
    public static final String THUMB_FORMAT = "webp";
    public static final String THUMB_CONTENT_TYPE = "image/webp";
    public static final String CACHE_CONTROL_IMMUTABLE = "public, max-age=31536000, immutable";
    public static final String DEFAULT_FOLDER = "uploads";
    public static final long UPLOAD_TIMEOUT_SECONDS = 30L;
}
