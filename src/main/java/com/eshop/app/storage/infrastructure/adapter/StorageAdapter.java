package com.eshop.app.storage.infrastructure.adapter;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.core.port.StoragePort;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageAdapter implements StoragePort {

    private final ObjectProvider<S3AsyncClient> s3AsyncClientProvider;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final AppProperties appProperties;

    @Value("${image.storage.provider:local}")
    private String provider;

    @Value("${app.storage.local.base-dir:${java.io.tmpdir}/eshop-uploads}")
    private String baseDir;

    @Value("${app.storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public String upload(String key, InputStream inputStream, String contentType, long sizeBytes) {
        if ("r2".equalsIgnoreCase(provider)) {
            S3AsyncClient s3AsyncClient = s3AsyncClientProvider.getIfAvailable();
            if (s3AsyncClient != null) {
                try {
                    byte[] bytes = inputStream.readAllBytes();
                    AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
                    PutObjectRequest putRequest =
                            PutObjectRequest.builder()
                                    .bucket(r2Config.getBucketName())
                                    .key(key)
                                    .contentType(contentType)
                                    .build();

                    CompletableFuture<Void> uploadFuture =
                            s3AsyncClient
                                    .putObject(putRequest, AsyncRequestBody.fromBytes(bytes))
                                    .thenAccept(
                                            response ->
                                                    log.debug(
                                                            "StoragePort uploaded to R2: {}", key));

                    uploadFuture.join();

                    String publicUrl = r2Config.getPublicUrl();
                    if (!publicUrl.endsWith("/")) {
                        publicUrl += "/";
                    }
                    return publicUrl + key;
                } catch (Exception e) {
                    log.error("Failed to upload to R2: {}", e.getMessage(), e);
                    throw new RuntimeException("R2 upload failed", e);
                }
            } else {
                log.warn(
                        "S3AsyncClient not available for R2 provider. Falling back to local"
                                + " storage.");
            }
        }

        // Local storage fallback
        try {
            Path target = Paths.get(baseDir, key);
            Files.createDirectories(target.getParent());
            byte[] bytes = inputStream.readAllBytes();
            Files.write(target, bytes);
            String url = baseUrl.endsWith("/") ? baseUrl + key : baseUrl + "/" + key;
            log.info("StoragePort uploaded locally: {}", key);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload locally: {}", e.getMessage(), e);
            throw new RuntimeException("Local upload failed", e);
        }
    }

    @Override
    public void delete(String key) {
        if ("r2".equalsIgnoreCase(provider)) {
            S3AsyncClient s3AsyncClient = s3AsyncClientProvider.getIfAvailable();
            if (s3AsyncClient != null) {
                AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
                try {
                    s3AsyncClient
                            .deleteObject(
                                    DeleteObjectRequest.builder()
                                            .bucket(r2Config.getBucketName())
                                            .key(key)
                                            .build())
                            .join();
                    log.info("StoragePort deleted from R2: {}", key);
                    return;
                } catch (Exception e) {
                    log.error("Failed to delete from R2: {}", e.getMessage(), e);
                }
            }
        }

        try {
            Path target = Paths.get(baseDir, key);
            Files.deleteIfExists(target);
            log.info("StoragePort deleted locally: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete locally: {}", e.getMessage(), e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, Duration duration) {
        if ("r2".equalsIgnoreCase(provider)) {
            S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
            if (s3Presigner != null) {
                AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
                try {
                    GetObjectPresignRequest presignRequest =
                            GetObjectPresignRequest.builder()
                                    .signatureDuration(duration)
                                    .getObjectRequest(
                                            builder ->
                                                    builder.bucket(r2Config.getBucketName())
                                                            .key(key)
                                                            .build())
                                    .build();
                    PresignedGetObjectRequest presigned =
                            s3Presigner.presignGetObject(presignRequest);
                    return presigned.url().toString();
                } catch (Exception e) {
                    log.error("Failed to presign URL: {}", e.getMessage(), e);
                }
            }
        }

        // Local fallback: return normal URL
        return baseUrl.endsWith("/") ? baseUrl + key : baseUrl + "/" + key;
    }

    @Override
    public boolean exists(String key) {
        if ("r2".equalsIgnoreCase(provider)) {
            S3AsyncClient s3AsyncClient = s3AsyncClientProvider.getIfAvailable();
            if (s3AsyncClient != null) {
                AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();
                try {
                    s3AsyncClient
                            .headObject(
                                    HeadObjectRequest.builder()
                                            .bucket(r2Config.getBucketName())
                                            .key(key)
                                            .build())
                            .join();
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }

        return Files.exists(Paths.get(baseDir, key));
    }
}
