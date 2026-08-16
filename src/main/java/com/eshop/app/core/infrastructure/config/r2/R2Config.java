package com.eshop.app.core.infrastructure.config.r2;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "image.storage.provider", havingValue = "r2", matchIfMissing = false)
public class R2Config {

    private final AppProperties appProperties;

    public R2Config(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        AppProperties.Storage.R2 r2 = appProperties.getStorage().getR2();
        
        if (r2 == null || r2.getAccessKeyId() == null || r2.getAccessKeyId().isBlank()
                || r2.getSecretAccessKey() == null || r2.getSecretAccessKey().isBlank()
                || r2.getAccountId() == null || r2.getAccountId().isBlank()) {
            return S3AsyncClient.builder()
                    .endpointOverride(URI.create("https://mock.r2.cloudflarestorage.com"))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("mock-key", "mock-secret")
                    ))
                    .forcePathStyle(true)
                    .build();
        }

        String endpoint = String.format("https://%s.r2.cloudflarestorage.com", r2.getAccountId());
        
        return S3AsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(r2.getRegion() != null && !r2.getRegion().isBlank() ? r2.getRegion() : "auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())
                ))
                // The SDK will automatically use NettyNioAsyncHttpClient if it's on the classpath
                .forcePathStyle(true)
                .build();
    }
}
