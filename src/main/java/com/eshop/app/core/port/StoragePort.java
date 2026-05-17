package com.eshop.app.core.port;

import java.io.InputStream;
import java.time.Duration;

/**
 * [HARDEN] Storage Port — outbound port for file/object storage.
 *
 * Decouples the application from specific storage providers (S3, R2, GCS, local).
 * All modules that need to store files MUST depend on this port, never the adapter directly.
 *
 * Enables:
 * - Provider-agnostic file operations
 * - Easy testing via mock/stub implementations
 * - Swap from R2 to S3 without modifying business code
 */
public interface StoragePort {

    /**
     * Uploads a file to the configured storage backend.
     *
     * @param key         the object key/path (e.g., "products/images/abc.jpg")
     * @param inputStream the file content stream
     * @param contentType the MIME type (e.g., "image/jpeg")
     * @param sizeBytes   the exact byte length of the stream
     * @return the public-accessible URL of the uploaded object
     */
    String upload(String key, InputStream inputStream, String contentType, long sizeBytes);

    /**
     * Deletes an object from storage.
     *
     * @param key the object key to delete
     */
    void delete(String key);

    /**
     * Generates a pre-signed URL for temporary direct access.
     *
     * @param key      the object key
     * @param duration how long the URL remains valid
     * @return a pre-signed URL
     */
    String generatePresignedUrl(String key, Duration duration);

    /**
     * Checks if an object exists in storage.
     *
     * @param key the object key
     * @return true if the object exists
     */
    boolean exists(String key);
}
