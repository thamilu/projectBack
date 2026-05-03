package com.eshop.app.storage;

import com.eshop.app.exception.ImageUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Local file system storage service for product images.
 * Stores images in ./uploads/products/{productId}/ directory.
 * Designed for easy migration to cloud storage (Cloudflare R2) in future.
 */
@Service("localStorageService")
@RequiredArgsConstructor
@Slf4j
public class LocalImageStorageService implements ImageStorageService {

    private final com.eshop.app.config.properties.AppProperties appProperties;

    private final Tika tika = new Tika();

    @Override
    public ImageUploadResult upload(byte[] bytes, String filename, String folder) throws IOException {
        // Validation
        validateFile(bytes, filename);

        // Create directory structure
        Path folderPath = Paths.get(appProperties.getStorage().getUploadDir(), folder);
        Files.createDirectories(folderPath);

        // Generate unique filename
        String sanitizedFileName = sanitizeFilename(filename);
        String extension = getFileExtension(sanitizedFileName).toLowerCase();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uniqueFilename = timestamp + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension;

        // Save original image
        Path imagePath = folderPath.resolve(uniqueFilename);
        Files.write(imagePath, bytes);

        // Read image dimensions
        BufferedImage bufferedImage = null;
        Integer width = null;
        Integer height = null;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            bufferedImage = ImageIO.read(bais);
            if (bufferedImage != null) {
                width = bufferedImage.getWidth();
                height = bufferedImage.getHeight();
            }
        } catch (Exception e) {
            log.warn("Could not read image dimensions for {}: {}", filename, e.getMessage());
        }

        // Generate thumbnail (150x150)
        String thumbnailFilename = "thumb_" + uniqueFilename;
        Path thumbnailPath = folderPath.resolve(thumbnailFilename);

        try {
            Thumbnails.of(imagePath.toFile())
                    .size(150, 150)
                    .keepAspectRatio(true)
                    .toFile(thumbnailPath.toFile());
        } catch (Exception e) {
            log.warn("Could not generate thumbnail for {}: {}", filename, e.getMessage());
            // If thumbnail generation fails, copy the original
            Files.copy(imagePath, thumbnailPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Build public URLs
        String imageKey = folder + "/" + uniqueFilename;
        String publicUrl = appProperties.getStorage().getBaseUrl() + "/uploads/" + folder + "/" + uniqueFilename;
        String thumbnailUrl = appProperties.getStorage().getBaseUrl() + "/uploads/" + folder + "/" + thumbnailFilename;

        log.info("Image uploaded successfully: {} -> {}", filename, imageKey);

        return ImageUploadResult.builder()
                .publicId(imageKey)
                .url(publicUrl)
                .thumbnailUrl(thumbnailUrl)
                .width(width)
                .height(height)
                .fileSize((long) bytes.length)
                .build();
    }

    @Override
    public void delete(String publicId, String folder) throws IOException {
        try {
            // publicId format: "products/{productId}/{filename}"
            Path imagePath = Paths.get(appProperties.getStorage().getUploadDir(), publicId);

            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                log.info("Image deleted: {}", publicId);

                // Try to delete thumbnail as well
                String filename = imagePath.getFileName().toString();
                Path thumbnailPath = imagePath.getParent().resolve("thumb_" + filename);
                if (Files.exists(thumbnailPath)) {
                    Files.delete(thumbnailPath);
                    log.info("Thumbnail deleted: thumb_{}", filename);
                }
            } else {
                log.warn("Image not found for deletion: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Failed to delete image: {}", publicId, e);
            throw e;
        }
    }

    /**
     * Validate file size and type.
     */
    private void validateFile(byte[] bytes, String filename) throws IOException {
        com.eshop.app.config.properties.AppProperties.Storage storage = appProperties.getStorage();

        // Check file size
        if (bytes.length > storage.getMaxFileSize()) {
            throw new ImageUploadException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            storage.getMaxFileSize() / 1024 / 1024));
        }

        // Detect MIME type using Apache Tika
        String detectedMimeType = tika.detect(bytes, filename);
        List<String> allowedMimeTypes = Arrays.asList(storage.getAllowedMimeTypes().split(","));
        if (!allowedMimeTypes.contains(detectedMimeType)) {
            throw new ImageUploadException(
                    String.format("Invalid file type: %s. Allowed types: %s", detectedMimeType,
                            storage.getAllowedMimeTypes()));
        }

        // Also check file extension
        String extension = getFileExtension(filename).toLowerCase();
        List<String> allowedExtensions = Arrays.asList(storage.getAllowedExtensions().split(","));
        if (!allowedExtensions.contains(extension)) {
            throw new ImageUploadException(
                    String.format("Invalid file extension: %s. Allowed: %s", extension,
                            storage.getAllowedExtensions()));
        }
    }

    /**
     * Sanitize filename to prevent directory traversal and special characters.
     */
    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "image";
        }

        // Remove path separators and special characters
        String sanitized = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Prevent directory traversal
        sanitized = sanitized.replace("..", "");
        sanitized = sanitized.replace("/", "");
        sanitized = sanitized.replace("\\", "");

        // Limit length
        if (sanitized.length() > 100) {
            String ext = getFileExtension(sanitized);
            sanitized = sanitized.substring(0, 90) + "." + ext;
        }

        return sanitized;
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // default
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
