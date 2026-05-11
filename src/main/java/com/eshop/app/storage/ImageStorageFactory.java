package com.eshop.app.storage;

import org.springframework.stereotype.Component;

@Component
public class ImageStorageFactory {

    private final R2StorageService r2Service;

    public ImageStorageFactory(R2StorageService r2Service) {
        this.r2Service = r2Service;
    }

    public ImageStorageService get() {
        // Cloudflare R2 is now the sole storage provider
        return r2Service;
    }
}
