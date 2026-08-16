package com.eshop.app.media.infrastructure.config;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * S3 Presigner configuration for generating presigned upload URLs.
 *
 * <p>Works alongside the existing {@code R2Config} (which creates {@code S3AsyncClient}). The
 * presigner is used exclusively for generating signed PUT URLs for direct-to-R2 uploads from the
 * frontend.
 *
 * <p>Only activated when {@code image.storage.provider=r2}.
 */
@Configuration
@ConditionalOnProperty(name = "image.storage.provider", havingValue = "r2", matchIfMissing = false)
public class S3PresignerConfig {

    private final AppProperties appProperties;

    public S3PresignerConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public S3Presigner s3Presigner() {
        AppProperties.Storage.R2 r2 = appProperties.getStorage().getR2();

        if (r2 == null || r2.getAccessKeyId() == null || r2.getAccessKeyId().isBlank()
                || r2.getSecretAccessKey() == null || r2.getSecretAccessKey().isBlank()
                || r2.getAccountId() == null || r2.getAccountId().isBlank()) {
            return S3Presigner.builder()
                    .endpointOverride(URI.create("https://mock.r2.cloudflarestorage.com"))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock-key", "mock-secret")
                    ))
                    .build();
        }

        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2.getAccountId());

        return S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .region(
                        Region.of(
                                r2.getRegion() != null && !r2.getRegion().isBlank()
                                        ? r2.getRegion()
                                        : "auto"))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        r2.getAccessKeyId(), r2.getSecretAccessKey())))
                .build();
    }
}
