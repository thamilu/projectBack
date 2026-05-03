package com.eshop.app.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageStorageFactory {

    private final CloudinaryStorageService cloudinaryService;
    private final BunnyNetStorageService bunnyService;
    private final LocalImageStorageService localService;

    @Value("${image.storage.provider:local}")
    private String provider;

    public ImageStorageFactory(
            CloudinaryStorageService cloudinaryService, 
            BunnyNetStorageService bunnyService,
            LocalImageStorageService localService) {
        this.cloudinaryService = cloudinaryService;
        this.bunnyService = bunnyService;
        this.localService = localService;
    }

    public ImageStorageService get() {
        if (provider == null) return localService;
        switch (provider.toLowerCase()) {
            case "local":
            case "filesystem":
            case "file":
                return localService;
            case "bunny":
            case "bunny.net":
            case "bunnycdn":
                return bunnyService;
            case "cloudinary":
                return cloudinaryService;
            default:
                return localService;
        }
    }
}
