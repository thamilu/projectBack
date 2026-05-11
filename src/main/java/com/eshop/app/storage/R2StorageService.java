package com.eshop.app.storage;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.exception.ImageUploadException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Hardened Cloudflare R2 Storage Service (Enterprise Async Implementation).
 * Uses S3AsyncClient for non-blocking, parallelized uploads and maximum scalability.
 */
@Service("r2StorageService")
@RequiredArgsConstructor
@Slf4j
public class R2StorageService implements ImageStorageService {

    private final S3AsyncClient s3AsyncClient;
    private final AppProperties appProperties;
    private final Tika tika = new Tika();

    private static final String R2_RETRY_INSTANCE = "r2Storage";

    @Override
    @Retry(name = R2_RETRY_INSTANCE)
    @CircuitBreaker(name = R2_RETRY_INSTANCE)
    public ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException {
        validateFile(bytes, filename);

        AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
        String sanitizedFolder = sanitizePath(folder);
        String uniqueFilename = generateUniqueFilename(filename);
        String key = sanitizedFolder + "/" + uniqueFilename;
        String thumbKey = sanitizedFolder + "/thumb_" + uniqueFilename;
        String contentType = tika.detect(bytes, filename);

        // Metadata extraction
        int[] dimensions = getImageDimensions(bytes);
        byte[] thumbBytes = generateThumbnail(bytes);

        // ═══════════════════════════════════════════════════════════
        // ENTERPRISE HARDENING: Parallel Non-Blocking Upload
        // ═══════════════════════════════════════════════════════════
        CompletableFuture<Void> originalUpload = uploadAsync(bytes, key, contentType, r2Config.getBucketName());
        CompletableFuture<Void> thumbUpload = uploadAsync(thumbBytes, thumbKey, "image/webp", r2Config.getBucketName());

        try {
            // Join both tasks - they run in parallel on Netty threads
            CompletableFuture.allOf(originalUpload, thumbUpload).join();
        } catch (Exception e) {
            log.error("R2 [PARALLEL UPLOAD FAILED]: {}", e.getMessage());
            throw new ImageUploadException("Cloudflare R2 async upload failed", e);
        }

        String baseUrl = r2Config.getPublicUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";

        log.info("R2 [ASYNC SUCCESS]: key={}, folder={}", key, folder);

        return ImageUploadResult.builder()
                .publicId(key)
                .url(baseUrl + key)
                .thumbnailUrl(baseUrl + thumbKey)
                .width(dimensions[0])
                .height(dimensions[1])
                .fileSize((long) bytes.length)
                .build();
    }

    private CompletableFuture<Void> uploadAsync(byte[] bytes, String key, String contentType, String bucket) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .cacheControl("public, max-age=31536000, immutable")
                .contentDisposition("inline; filename=\"" + FilenameUtils.getName(key) + "\"")
                .build();

        return s3AsyncClient.putObject(putRequest, AsyncRequestBody.fromBytes(bytes))
                .thenAccept(response -> log.debug("R2 [PARTIAL SUCCESS]: {}", key))
                .exceptionally(ex -> {
                    log.error("R2 [PARTIAL FAILURE]: key={}, error={}", key, ex.getMessage());
                    throw new RuntimeException(ex);
                });
    }

    @Override
    @Retry(name = R2_RETRY_INSTANCE)
    public void delete(String publicId, String folder) throws IOException {
        AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
        
        try {
            String thumbKey = publicId.replaceFirst("([^/]+)$", "thumb_$1");
            
            CompletableFuture<Void> deleteOriginal = s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(publicId)
                    .build()).thenAccept(r -> {});

            CompletableFuture<Void> deleteThumb = s3AsyncClient.deleteObject(DeleteObjectRequest.builder()
                    .bucket(r2Config.getBucketName())
                    .key(thumbKey)
                    .build()).thenAccept(r -> {});

            CompletableFuture.allOf(deleteOriginal, deleteThumb).join();
            
            log.info("R2 [DELETE SUCCESS]: {}", publicId);
        } catch (Exception e) {
            log.error("R2 [DELETE FAILED]: publicId={}, error={}", publicId, e.getMessage());
            throw new IOException("R2 async deletion failed", e);
        }
    }

    private void validateFile(byte[] bytes, String filename) {
        AppProperties.Storage storage = appProperties.getStorage();
        if (bytes == null || bytes.length == 0) throw new ImageUploadException("Empty file");
        if (bytes.length > storage.getMaxFileSize()) throw new ImageUploadException("File too large");

        String mime = tika.detect(bytes, filename);
        if (!Arrays.asList(storage.getAllowedMimeTypes().split(",")).contains(mime)) {
            throw new ImageUploadException("Invalid MIME type: " + mime);
        }
    }

    private String generateUniqueFilename(String originalFilename) {
        return UUID.randomUUID().toString().replace("-", "") + "." + 
               FilenameUtils.getExtension(originalFilename).toLowerCase();
    }

    private String sanitizePath(String path) {
        if (path == null) return "uploads";
        return path.replaceAll("[^a-zA-Z0-9/._-]", "_").replaceAll("/+", "/").replaceAll("^/|/$", "");
    }

    private byte[] generateThumbnail(byte[] originalBytes) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(bais).size(250, 250).outputFormat("webp").outputQuality(0.8).toOutputStream(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return originalBytes;
        }
    }

    private int[] getImageDimensions(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            return img != null ? new int[]{img.getWidth(), img.getHeight()} : new int[]{0, 0};
        } catch (Exception e) {
            return new int[]{0, 0};
        }
    }
}
