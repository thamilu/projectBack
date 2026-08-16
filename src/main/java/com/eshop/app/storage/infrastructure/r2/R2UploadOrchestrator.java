package com.eshop.app.storage.infrastructure.r2;

import com.eshop.app.storage.config.StorageConstants;
import com.eshop.app.storage.exception.ImageUploadException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@ConditionalOnProperty(name = "image.storage.provider", havingValue = "r2", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class R2UploadOrchestrator {

    private final S3AsyncClient s3AsyncClient;

    public void uploadParallel(
            byte[] bytes,
            byte[] thumbBytes,
            String key,
            String thumbKey,
            String contentType,
            String bucketName) {

        CompletableFuture<Void> originalUpload = uploadAsync(bytes, key, contentType, bucketName);
        CompletableFuture<Void> thumbUpload =
                uploadAsync(thumbBytes, thumbKey, StorageConstants.THUMB_CONTENT_TYPE, bucketName);

        CompletableFuture<Void> combined =
                CompletableFuture.allOf(originalUpload, thumbUpload)
                        .orTimeout(StorageConstants.UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .exceptionally(
                                ex -> {
                                    log.error(
                                            "R2 [PARALLEL UPLOAD FAILED] - initiating compensating"
                                                    + " rollback delete: {}",
                                            ex.getMessage());
                                    rollbackUploads(key, thumbKey, bucketName);
                                    throw new CompletionException(
                                            new ImageUploadException(
                                                    "Cloudflare R2 parallel upload failed,"
                                                            + " rollback initiated",
                                                    ex));
                                });

        try {
            combined.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof ImageUploadException) {
                throw (ImageUploadException) e.getCause();
            }
            throw new ImageUploadException("Cloudflare R2 upload failed", e);
        } catch (Exception e) {
            throw new ImageUploadException("Cloudflare R2 upload failed", e);
        }
    }

    public void deleteParallel(String bucket, String key, String thumbKey) {
        try {
            CompletableFuture.allOf(deleteKeyAsync(bucket, key), deleteKeyAsync(bucket, thumbKey))
                    .orTimeout(StorageConstants.UPLOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
            log.info("R2 [DELETE SUCCESS]: {}", key);
        } catch (Exception e) {
            log.error("R2 [DELETE FAILED]: key={}, error={}", key, e.getMessage());
            throw new ImageUploadException("R2 deletion failed", e);
        }
    }

    private CompletableFuture<Void> uploadAsync(
            byte[] bytes, String key, String contentType, String bucket) {

        String encodedFilename =
                URLEncoder.encode(FilenameUtils.getName(key), StandardCharsets.UTF_8);

        PutObjectRequest putRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .cacheControl(StorageConstants.CACHE_CONTROL_IMMUTABLE)
                        .contentDisposition("inline; filename*=UTF-8''" + encodedFilename)
                        .build();

        return s3AsyncClient
                .putObject(putRequest, AsyncRequestBody.fromBytes(bytes))
                .thenAccept(response -> log.debug("R2 [PARTIAL SUCCESS]: {}", key))
                .exceptionally(
                        ex -> {
                            log.error(
                                    "R2 [PARTIAL FAILURE]: key={}, error={}", key, ex.getMessage());
                            throw new CompletionException(ex);
                        });
    }

    private CompletableFuture<Void> deleteKeyAsync(String bucket, String key) {
        return s3AsyncClient
                .deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
                .thenAccept(r -> log.debug("R2 [DELETE PARTIAL]: key={}", key))
                .exceptionally(
                        ex -> {
                            log.error(
                                    "R2 [DELETE PARTIAL FAILED]: key={}, error={}",
                                    key,
                                    ex.getMessage());
                            throw new CompletionException(ex);
                        });
    }

    private void rollbackUploads(String key, String thumbKey, String bucket) {
        List.of(key, thumbKey)
                .forEach(
                        k -> {
                            try {
                                s3AsyncClient
                                        .deleteObject(
                                                DeleteObjectRequest.builder()
                                                        .bucket(bucket)
                                                        .key(k)
                                                        .build())
                                        .join();
                                log.warn("R2 [ROLLBACK]: deleted orphaned key={}", k);
                            } catch (Exception ex) {
                                log.error(
                                        "R2 [ROLLBACK FAILED]: key={}, error={}",
                                        k,
                                        ex.getMessage());
                            }
                        });
    }
}
