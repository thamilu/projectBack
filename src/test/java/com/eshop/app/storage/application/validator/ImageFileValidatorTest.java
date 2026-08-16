package com.eshop.app.storage.application.validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.storage.exception.ImageUploadException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImageFileValidatorTest {

    @Mock private Tika tika;

    @Mock private AppProperties appProperties;

    @Mock private AppProperties.Storage storage;

    private SimpleMeterRegistry meterRegistry;
    private ImageFileValidator validator;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        when(appProperties.getStorage()).thenReturn(storage);
        when(storage.getMaxFileSize()).thenReturn(1024 * 1024L); // 1MB
        when(storage.getMaxFilenameLength()).thenReturn(255);
        when(storage.getAllowedMimeTypes()).thenReturn("image/jpeg,image/png,image/gif,image/webp");
        validator = new ImageFileValidator(tika, appProperties, meterRegistry);
        validator.init();
    }

    private byte[] createTestImageBytes(int width, int height, String format) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    @Test
    void validate_WithValidJpeg_ShouldPass() throws Exception {
        byte[] jpegBytes = createTestImageBytes(10, 10, "jpeg");
        // Ensure JPEG SOI header is present
        jpegBytes[0] = (byte) 0xFF;
        jpegBytes[1] = (byte) 0xD8;
        jpegBytes[2] = (byte) 0xFF;
        jpegBytes[3] = (byte) 0xE0;

        when(tika.detect(jpegBytes)).thenReturn("image/jpeg");
        when(tika.detect("test.jpg")).thenReturn("image/jpeg");

        String mime = validator.validate(jpegBytes, "test.jpg");

        assertThat(mime).isEqualTo("image/jpeg");
        assertThat(
                        meterRegistry
                                .counter("image.validation.success", "mime_type", "image/jpeg")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithValidPng_ShouldPass() throws Exception {
        byte[] pngBytes = createTestImageBytes(10, 10, "png");
        // Ensure PNG 8-byte header is present
        pngBytes[0] = (byte) 0x89;
        pngBytes[1] = (byte) 0x50;
        pngBytes[2] = (byte) 0x4E;
        pngBytes[3] = (byte) 0x47;
        pngBytes[4] = (byte) 0x0D;
        pngBytes[5] = (byte) 0x0A;
        pngBytes[6] = (byte) 0x1A;
        pngBytes[7] = (byte) 0x0A;

        when(tika.detect(pngBytes)).thenReturn("image/png");
        when(tika.detect("test.png")).thenReturn("image/png");

        String mime = validator.validate(pngBytes, "test.png");

        assertThat(mime).isEqualTo("image/png");
        assertThat(
                        meterRegistry
                                .counter("image.validation.success", "mime_type", "image/png")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithValidGif87a_ShouldPass() throws Exception {
        byte[] gifBytes = createTestImageBytes(10, 10, "gif");
        // Set GIF87a magic bytes
        gifBytes[0] = 0x47;
        gifBytes[1] = 0x49;
        gifBytes[2] = 0x46;
        gifBytes[3] = 0x38;
        gifBytes[4] = 0x37;
        gifBytes[5] = 0x61;

        when(tika.detect(gifBytes)).thenReturn("image/gif");
        when(tika.detect("test.gif")).thenReturn("image/gif");

        String mime = validator.validate(gifBytes, "test.gif");

        assertThat(mime).isEqualTo("image/gif");
    }

    @Test
    void validate_WithValidGif89a_ShouldPass() throws Exception {
        byte[] gifBytes = createTestImageBytes(10, 10, "gif");
        // Set GIF89a magic bytes
        gifBytes[0] = 0x47;
        gifBytes[1] = 0x49;
        gifBytes[2] = 0x46;
        gifBytes[3] = 0x38;
        gifBytes[4] = 0x39;
        gifBytes[5] = 0x61;

        when(tika.detect(gifBytes)).thenReturn("image/gif");
        when(tika.detect("test.gif")).thenReturn("image/gif");

        String mime = validator.validate(gifBytes, "test.gif");

        assertThat(mime).isEqualTo("image/gif");
    }

    @Test
    void validate_WithValidWebp_ShouldPass() throws Exception {
        // Prepare minimum WebP-like byte structure
        byte[] webpBytes = new byte[12];
        webpBytes[0] = 0x52; // R
        webpBytes[1] = 0x49; // I
        webpBytes[2] = 0x46; // F
        webpBytes[3] = 0x46; // F
        webpBytes[8] = 0x57; // W
        webpBytes[9] = 0x45; // E
        webpBytes[10] = 0x42; // B
        webpBytes[11] = 0x50; // P

        when(tika.detect(webpBytes)).thenReturn("image/webp");
        when(tika.detect("test.webp")).thenReturn("image/webp");

        String mime = validator.validate(webpBytes, "test.webp");

        assertThat(mime).isEqualTo("image/webp");
    }

    @Test
    void validate_WithEmptyFile_ShouldThrow() {
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(new byte[0], "test.jpg");
                        });
        assertThat(ex.getMessage()).isEqualTo("File must not be empty");
        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "file_empty")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithTooSmallFile_ShouldThrow() {
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(new byte[5], "test.jpg");
                        });
        assertThat(ex.getMessage()).isEqualTo("File too small to be a valid image");
        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "file_too_small")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithTooLargeFile_ShouldThrow() {
        byte[] largeBytes = new byte[2000000]; // 2MB
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(largeBytes, "test.jpg");
                        });
        assertThat(ex.getMessage()).contains("exceeds max allowed");
        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "file_too_large")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithBlankFilename_ShouldThrow() {
        byte[] bytes = new byte[12];
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "   ");
                        });
        assertThat(ex.getMessage()).isEqualTo("Filename must not be blank");
        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "filename_blank")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithTooLongFilename_ShouldThrow() {
        byte[] bytes = new byte[12];
        String longName = "a".repeat(256) + ".jpg";
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, longName);
                        });
        assertThat(ex.getMessage()).contains("Filename too long");
        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "filename_too_long")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithPathTraversalFilename_ShouldThrow() {
        byte[] bytes = new byte[12];
        ImageUploadException ex1 =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "../test.jpg");
                        });
        assertThat(ex1.getMessage()).isEqualTo("Filename contains illegal characters");

        ImageUploadException ex2 =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "sub/test.jpg");
                        });
        assertThat(ex2.getMessage()).isEqualTo("Filename contains illegal characters");

        assertThat(
                        meterRegistry
                                .counter("image.validation.rejected", "reason", "path_traversal")
                                .count())
                .isEqualTo(2);
    }

    @Test
    void validate_WithMissingExtensionFilename_ShouldThrow() {
        byte[] bytes = new byte[12];
        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "test");
                        });
        assertThat(ex.getMessage()).isEqualTo("Filename must have a valid extension");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "filename_missing_extension")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithUnsupportedMimeType_ShouldThrow() {
        byte[] bytes = new byte[12];
        when(tika.detect(bytes)).thenReturn("application/pdf");

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "test.pdf");
                        });
        assertThat(ex.getMessage())
                .isEqualTo("File type not supported. Please upload a valid image.");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "mime_type_not_allowed")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithSpoofedWebpMagicBytes_ShouldThrow() {
        // "RIFF" header but no "WEBP" header (claims to be WebP)
        byte[] webpBytes = new byte[12];
        webpBytes[0] = 0x52; // R
        webpBytes[1] = 0x49; // I
        webpBytes[2] = 0x46; // F
        webpBytes[3] = 0x46; // F
        // bytes 8-11 are 0, not "WEBP"

        when(tika.detect(webpBytes)).thenReturn("image/webp");

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(webpBytes, "test.webp");
                        });
        assertThat(ex.getMessage()).isEqualTo("File signature does not match its declared type.");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "magic_bytes_mismatch")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithSpoofedJpegMagicBytes_ShouldThrow() throws Exception {
        byte[] bytes = createTestImageBytes(10, 10, "jpeg");
        // Corrupt first JPEG bytes
        bytes[0] = 0x00;
        bytes[1] = 0x00;

        when(tika.detect(bytes)).thenReturn("image/jpeg");

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "test.jpg");
                        });
        assertThat(ex.getMessage()).isEqualTo("File signature does not match its declared type.");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "magic_bytes_mismatch")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithSpoofedGifMagicBytes_ShouldThrow() throws Exception {
        byte[] bytes = createTestImageBytes(10, 10, "gif");
        // Corrupt GIF signature
        bytes[0] = 0x00;

        when(tika.detect(bytes)).thenReturn("image/gif");

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "test.gif");
                        });
        assertThat(ex.getMessage()).isEqualTo("File signature does not match its declared type.");
    }

    @Test
    void validate_WithImageBombWidth_ShouldThrow() throws Exception {
        byte[] bytes = createTestImageBytes(10001, 1, "png");
        // Ensure PNG header matches signature checks
        bytes[0] = (byte) 0x89;
        bytes[1] = (byte) 0x50;
        bytes[2] = (byte) 0x4E;
        bytes[3] = (byte) 0x47;
        bytes[4] = (byte) 0x0D;
        bytes[5] = (byte) 0x0A;
        bytes[6] = (byte) 0x1A;
        bytes[7] = (byte) 0x0A;

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "bomb.png");
                        });
        assertThat(ex.getMessage()).isEqualTo("Image dimensions exceed maximum allowed size");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "image_dimensions_too_large")
                                .count())
                .isEqualTo(1);
    }

    @Test
    void validate_WithImageBombHeight_ShouldThrow() throws Exception {
        byte[] bytes = createTestImageBytes(1, 10001, "png");
        // Ensure PNG header matches signature checks
        bytes[0] = (byte) 0x89;
        bytes[1] = (byte) 0x50;
        bytes[2] = (byte) 0x4E;
        bytes[3] = (byte) 0x47;
        bytes[4] = (byte) 0x0D;
        bytes[5] = (byte) 0x0A;
        bytes[6] = (byte) 0x1A;
        bytes[7] = (byte) 0x0A;

        ImageUploadException ex =
                assertThrows(
                        ImageUploadException.class,
                        () -> {
                            validator.validate(bytes, "bomb.png");
                        });
        assertThat(ex.getMessage()).isEqualTo("Image dimensions exceed maximum allowed size");
        assertThat(
                        meterRegistry
                                .counter(
                                        "image.validation.rejected",
                                        "reason",
                                        "image_dimensions_too_large")
                                .count())
                .isEqualTo(1);
    }
}
