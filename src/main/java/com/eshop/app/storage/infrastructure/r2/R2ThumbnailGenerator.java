package com.eshop.app.storage.infrastructure.r2;

import com.eshop.app.storage.config.StorageConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class R2ThumbnailGenerator {

    public byte[] generateThumbnail(byte[] originalBytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalBytes);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Thumbnails.of(bais)
                    .size(StorageConstants.THUMB_WIDTH, StorageConstants.THUMB_HEIGHT)
                    .outputFormat(StorageConstants.THUMB_FORMAT)
                    .outputQuality(StorageConstants.THUMB_QUALITY)
                    .toOutputStream(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("R2 [THUMBNAIL FAILED]: falling back to original. reason={}", e.getMessage());
            return originalBytes;
        }
    }
}
