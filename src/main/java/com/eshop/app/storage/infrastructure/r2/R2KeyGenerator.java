package com.eshop.app.storage.infrastructure.r2;

import com.eshop.app.storage.config.StorageConstants;
import com.eshop.app.storage.exception.ImageUploadException;
import java.nio.file.Paths;
import java.util.UUID;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
public class R2KeyGenerator {

    public String generateUniqueKey(String filename, String folder) {
        String sanitizedFolder = sanitizePath(folder);
        String uniqueFilename = generateUniqueFilename(filename);
        return sanitizedFolder + "/" + uniqueFilename;
    }

    public String toThumbnailKey(String originalKey) {
        if (originalKey == null) {
            return null;
        }
        int lastSlash = originalKey.lastIndexOf('/');
        if (lastSlash == -1) {
            return "thumb_" + originalKey;
        }
        return originalKey.substring(0, lastSlash + 1)
                + "thumb_"
                + originalKey.substring(lastSlash + 1);
    }

    private String generateUniqueFilename(String originalFilename) {
        String ext = FilenameUtils.getExtension(originalFilename);
        String cleanExt = ext != null ? ext.toLowerCase() : "";
        return UUID.randomUUID().toString().replace("-", "") + "." + cleanExt;
    }

    private String sanitizePath(String path) {
        if (path == null || path.isBlank()) {
            return StorageConstants.DEFAULT_FOLDER;
        }

        // Normalize first to resolve any encoded traversal
        String normalized = Paths.get(path).normalize().toString().replace('\\', '/');

        // Reject if traversal detected after normalization
        if (normalized.contains("..")) {
            throw new ImageUploadException("Invalid folder path detected");
        }

        return normalized
                .replaceAll("[^a-zA-Z0-9/._-]", "_")
                .replaceAll("/+", "/")
                .replaceAll("^/|/$", "");
    }
}
