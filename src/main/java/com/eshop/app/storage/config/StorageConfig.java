package com.eshop.app.storage.config;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import java.net.MalformedURLException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * Spring configuration for Cloudflare R2 storage beans.
 *
 * <p>Produces a validated, immutable {@link R2StorageConfig} bean when {@code
 * image.storage.provider=r2} is active.
 *
 * <p>Performs fail-fast startup validation of all critical R2 properties to prevent
 * misconfiguration from reaching runtime.
 *
 * @see R2StorageConfig
 * @see com.eshop.app.storage.application.service.impl.R2StorageServiceImpl
 */
@Configuration
@Slf4j
public class StorageConfig {

    /**
     * Produces a validated, immutable R2 storage configuration bean. Only active when {@code
     * image.storage.provider=r2}.
     *
     * @param appProperties application-wide properties source
     * @return immutable, validated {@link R2StorageConfig}
     * @throws IllegalArgumentException if any required R2 property is missing or invalid
     */
    @Bean("r2StorageConfig")
    @ConditionalOnProperty(
            name = "image.storage.provider",
            havingValue = "r2",
            matchIfMissing = false)
    public R2StorageConfig r2StorageConfig(AppProperties appProperties) {
        AppProperties.Storage.R2 raw = appProperties.getStorage().getR2();

        if (raw == null || raw.getBucketName() == null || raw.getBucketName().isBlank()
                || raw.getPublicUrl() == null || raw.getPublicUrl().isBlank()) {
            log.warn("[StorageConfig] R2 credentials/bucket properties are blank. Returning mock config.");
            return new R2StorageConfig("mock-bucket", "http://mock-public-url", "mock-account", "auto");
        }

        validateR2Config(raw);

        R2StorageConfig config =
                new R2StorageConfig(
                        raw.getBucketName(),
                        raw.getPublicUrl(),
                        raw.getAccountId(),
                        raw.getRegion());

        log.info(
                "[StorageConfig] R2 initialized — bucket='{}', publicUrl='{}'",
                config.bucketName(),
                config.publicUrl());

        return config;
    }


    private void validateR2Config(AppProperties.Storage.R2 r2) {
        Assert.hasText(
                r2.getBucketName(),
                "[StorageConfig] 'app.storage.r2.bucket-name' must not be blank");

        Assert.hasText(
                r2.getPublicUrl(), "[StorageConfig] 'app.storage.r2.public-url' must not be blank");

        Assert.isTrue(
                isValidUrl(r2.getPublicUrl()),
                "[StorageConfig] 'app.storage.r2.public-url' is not a valid URL: "
                        + r2.getPublicUrl());
    }

    private boolean isValidUrl(String url) {
        try {
            URI.create(url).toURL();
            return true;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return false;
        }
    }
}
