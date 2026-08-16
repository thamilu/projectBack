package com.eshop.app.storage.application.validator;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.storage.exception.ImageUploadException;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/**
 * Validates uploaded image files for size, filename safety, and content type before storage
 * operations.
 *
 * <p>MIME detection uses content-only analysis (no filename hint) to prevent extension spoofing
 * attacks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageFileValidator {

    private final Tika tika;
    private final AppProperties appProperties;
    private final MeterRegistry meterRegistry;

    // ─── Cached at startup ───────────────────────────────────
    private Set<String> allowedMimeTypes;
    private long maxFileSize;
    private int maxFilenameLength;

    // ─── Constants ───────────────────────────────────────────
    private static final int MAX_IMAGE_DIMENSION = 10000;
    private static final int MIN_FILE_SIZE = 12;

    private static final Map<String, byte[][]> IMAGE_MAGIC_BYTES =
            Map.of(
                    "image/jpeg", new byte[][] {{(byte) 0xFF, (byte) 0xD8}},
                    "image/png",
                            new byte[][] {{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}},
                    "image/gif",
                            new byte[][] {
                                {0x47, 0x49, 0x46, 0x38, 0x37, 0x61}, // GIF87a
                                {0x47, 0x49, 0x46, 0x38, 0x39, 0x61} // GIF89a
                            },
                    "image/webp",
                            new byte[][] {
                                {0x52, 0x49, 0x46, 0x46}, // "RIFF" at offset 0
                                {0x57, 0x45, 0x42, 0x50} // "WEBP" at offset 8
                            });

    // ─── Initialization ──────────────────────────────────────

    @PostConstruct
    void init() {
        AppProperties.Storage storage = appProperties.getStorage();
        this.maxFileSize = storage.getMaxFileSize();
        this.maxFilenameLength =
                storage.getMaxFilenameLength() > 0 ? storage.getMaxFilenameLength() : 255;

        String config = storage.getAllowedMimeTypes();
        this.allowedMimeTypes =
                (config != null && !config.isBlank())
                        ? Arrays.stream(config.split(","))
                                .filter(Objects::nonNull)
                                .map(s -> s.trim())
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toUnmodifiableSet())
                        : Set.of();

        if (allowedMimeTypes.isEmpty()) {
            log.warn("[ImageValidator] No MIME types configured — all uploads will be rejected");
        } else {
            log.info(
                    "[ImageValidator] Initialized — allowedTypes={}, maxFileSize={},"
                            + " maxFilenameLength={}",
                    allowedMimeTypes,
                    maxFileSize,
                    maxFilenameLength);
        }
    }

    // ─── Public API ──────────────────────────────────────────

    /**
     * Validates the file and returns the detected MIME type.
     *
     * @param bytes raw file bytes
     * @param filename original filename (used for extension check only)
     * @return detected MIME type string
     * @throws ImageUploadException if validation fails
     */
    public String validate(byte[] bytes, String filename) {
        validateNotEmpty(bytes);
        validateFilename(filename);
        validateFileSize(bytes);
        validateImageDimensions(bytes);
        String mime = validateAndResolveMime(bytes, filename);
        meterRegistry.counter("image.validation.success", "mime_type", mime).increment();
        return mime;
    }

    // ─── Private Validators ──────────────────────────────────

    private void validateNotEmpty(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            meterRegistry.counter("image.validation.rejected", "reason", "file_empty").increment();
            throw new ImageUploadException("File must not be empty");
        }
        if (bytes.length < MIN_FILE_SIZE) {
            meterRegistry
                    .counter("image.validation.rejected", "reason", "file_too_small")
                    .increment();
            throw new ImageUploadException("File too small to be a valid image");
        }
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            meterRegistry
                    .counter("image.validation.rejected", "reason", "filename_blank")
                    .increment();
            throw new ImageUploadException("Filename must not be blank");
        }
        if (filename.length() > maxFilenameLength) {
            meterRegistry
                    .counter("image.validation.rejected", "reason", "filename_too_long")
                    .increment();
            throw new ImageUploadException(
                    "Filename too long: max " + maxFilenameLength + " characters");
        }
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            log.warn("[ImageValidator] Suspicious filename rejected: '{}'", filename);
            meterRegistry
                    .counter("image.validation.rejected", "reason", "path_traversal")
                    .increment();
            throw new ImageUploadException("Filename contains illegal characters");
        }
        if (FilenameUtils.getExtension(filename).isBlank()) {
            meterRegistry
                    .counter("image.validation.rejected", "reason", "filename_missing_extension")
                    .increment();
            throw new ImageUploadException("Filename must have a valid extension");
        }
    }

    private void validateFileSize(byte[] bytes) {
        if (bytes.length > maxFileSize) {
            log.warn("[ImageValidator] File too large: size={}, max={}", bytes.length, maxFileSize);
            meterRegistry
                    .counter("image.validation.rejected", "reason", "file_too_large")
                    .increment();
            throw new ImageUploadException(
                    String.format(
                            "File size %d exceeds max allowed %d", bytes.length, maxFileSize));
        }
    }

    private void validateImageDimensions(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img != null) {
                if (img.getWidth() > MAX_IMAGE_DIMENSION || img.getHeight() > MAX_IMAGE_DIMENSION) {
                    log.warn(
                            "[ImageValidator] Image dimensions too large: {}x{}",
                            img.getWidth(),
                            img.getHeight());
                    meterRegistry
                            .counter(
                                    "image.validation.rejected",
                                    "reason",
                                    "image_dimensions_too_large")
                            .increment();
                    throw new ImageUploadException("Image dimensions exceed maximum allowed size");
                }
            }
        } catch (IOException e) {
            log.warn("[ImageValidator] Could not validate image dimensions", e);
            // Allow through if dimensions can't be checked (don't fail safe here)
        }
    }

    private String validateAndResolveMime(byte[] bytes, String filename) {
        // Content-only detection — never trust filename hint
        String detectedMime = tika.detect(bytes);

        if (detectedMime == null || !allowedMimeTypes.contains(detectedMime)) {
            log.warn("[ImageValidator] Blocked — MIME='{}', filename='{}'", detectedMime, filename);
            meterRegistry
                    .counter("image.validation.rejected", "reason", "mime_type_not_allowed")
                    .increment();
            throw new ImageUploadException("File type not supported. Please upload a valid image.");
        }

        // Secondary: warn on filename/content mismatch (possible spoofing)
        String filenameMime = tika.detect(filename);
        if (!detectedMime.equals(filenameMime)) {
            log.warn(
                    "[ImageValidator] MIME mismatch — content='{}', filename hints '{}'",
                    detectedMime,
                    filenameMime);
        }

        // Validate magic bytes for known types
        validateMagicBytes(bytes, detectedMime);

        return detectedMime;
    }

    private void validateMagicBytes(byte[] bytes, String mime) {
        byte[][] magicSequences = IMAGE_MAGIC_BYTES.get(mime);
        if (magicSequences == null) return;

        if (mime.equals("image/webp")) {
            if (bytes.length < 12) {
                meterRegistry
                        .counter("image.validation.rejected", "reason", "magic_bytes_mismatch")
                        .increment();
                throw new ImageUploadException("File too small to be a valid WebP");
            }
            validateMagicSequence(bytes, magicSequences[0], 0, mime); // "RIFF" at 0
            validateMagicSequence(bytes, magicSequences[1], 8, mime); // "WEBP" at 8
        } else if (mime.equals("image/gif")) {
            boolean isGif87a = matchesMagic(bytes, magicSequences[0], 0);
            boolean isGif89a = matchesMagic(bytes, magicSequences[1], 0);
            if (!isGif87a && !isGif89a) {
                log.warn("[ImageValidator] Invalid GIF signature");
                meterRegistry
                        .counter("image.validation.rejected", "reason", "magic_bytes_mismatch")
                        .increment();
                throw new ImageUploadException("File signature does not match its declared type.");
            }
        } else if (mime.equals("image/jpeg")) {
            // Must start with FF D8
            validateMagicSequence(bytes, magicSequences[0], 0, mime);
            // Next segment check
            if (bytes.length < 4) {
                meterRegistry
                        .counter("image.validation.rejected", "reason", "magic_bytes_mismatch")
                        .increment();
                throw new ImageUploadException("File too small to contain valid JPEG headers");
            }
            if (bytes[2] != (byte) 0xFF) {
                log.warn(
                        "[ImageValidator] Invalid JPEG marker byte 2: {}",
                        String.format("%02X", bytes[2]));
                meterRegistry
                        .counter("image.validation.rejected", "reason", "magic_bytes_mismatch")
                        .increment();
                throw new ImageUploadException("File signature does not match its declared type.");
            }
            byte marker = bytes[3];
            if (marker != (byte) 0xE0
                    && marker != (byte) 0xE1
                    && marker != (byte) 0xDB
                    && marker != (byte) 0xEE) {
                log.warn("[ImageValidator] Unusual JPEG marker: {}", String.format("%02X", marker));
            }
        } else {
            // Other formats, check first sequence at offset 0
            validateMagicSequence(bytes, magicSequences[0], 0, mime);
        }
    }

    private boolean matchesMagic(byte[] bytes, byte[] expectedMagic, int offset) {
        if (bytes.length < offset + expectedMagic.length) {
            return false;
        }
        for (int i = 0; i < expectedMagic.length; i++) {
            if (bytes[offset + i] != expectedMagic[i]) {
                return false;
            }
        }
        return true;
    }

    private void validateMagicSequence(
            byte[] bytes, byte[] expectedMagic, int offset, String mime) {
        if (!matchesMagic(bytes, expectedMagic, offset)) {
            log.warn(
                    "[ImageValidator] Magic byte mismatch for MIME='{}' at offset {}",
                    mime,
                    offset);
            meterRegistry
                    .counter("image.validation.rejected", "reason", "magic_bytes_mismatch")
                    .increment();
            throw new ImageUploadException("File signature does not match its declared type.");
        }
    }
}
