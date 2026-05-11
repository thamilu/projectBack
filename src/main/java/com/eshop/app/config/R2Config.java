package com.eshop.app.config;

import com.eshop.app.config.properties.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.net.URI;

@Configuration
public class R2Config {

    private final AppProperties appProperties;

    public R2Config(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public S3AsyncClient s3AsyncClient() {
        AppProperties.Storage.R2 r2 = appProperties.getStorage().getR2();
        
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
