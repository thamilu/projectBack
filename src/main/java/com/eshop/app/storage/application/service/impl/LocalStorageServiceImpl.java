package com.eshop.app.storage.application.service.impl;

import com.eshop.app.storage.api.response.ImageUploadResult;
import com.eshop.app.storage.application.port.in.ImageStorageUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Local filesystem-based image storage service.
 * Active when {@code image.storage.provider=local} (default).
 */
@Service("localStorageService")
@ConditionalOnProperty(name = "image.storage.provider", havingValue = "local", matchIfMissing = true)
@Slf4j
public class LocalStorageServiceImpl implements ImageStorageUseCase {

    @Value("${app.storage.local.base-dir:${java.io.tmpdir}/eshop-uploads}")
    private String baseDir;

    @Value("${app.storage.local.base-url:http://localhost:8080/uploads}")
    private String baseUrl;

    @Override
    public ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException {
        String ext = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : "";
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + ext;
        String relPath = (folder != null ? folder : "uploads") + "/" + uniqueName;

        Path target = Paths.get(baseDir, relPath);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);

        String url = baseUrl.endsWith("/") ? baseUrl + relPath : baseUrl + "/" + relPath;
        log.info("LOCAL STORAGE [UPLOAD SUCCESS]: {}", relPath);

        return ImageUploadResult.builder()
                .publicId(relPath)
                .url(url)
                .thumbnailUrl(url)
                .width(0)
                .height(0)
                .fileSize((long) bytes.length)
                .build();
    }

    @Override
    public void delete(String publicId, String folder) throws IOException {
        Path target = Paths.get(baseDir, publicId);
        Files.deleteIfExists(target);
        log.info("LOCAL STORAGE [DELETE]: {}", publicId);
    }
}
