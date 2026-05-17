package com.eshop.app.storage.infrastructure.config;

import com.eshop.app.storage.application.port.in.ImageStorageUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Factory for selecting the active ImageStorageUseCase implementation
 * based on the configured {@code image.storage.provider} property.
 * <p>
 * Supports: {@code r2} (Cloudflare R2), {@code local} (filesystem).
 */
@Component
public class ImageStorageFactory {

    private final ApplicationContext applicationContext;
    private final String provider;

    public ImageStorageFactory(ApplicationContext applicationContext,
            @Value("${image.storage.provider:local}") String provider) {
        this.applicationContext = applicationContext;
        this.provider = provider;
    }

    public ImageStorageUseCase get() {
        String beanName = switch (provider.toLowerCase()) {
            case "r2" -> "r2StorageService";
            default -> "localStorageService";
        };
        return applicationContext.getBean(beanName, ImageStorageUseCase.class);
    }
}
