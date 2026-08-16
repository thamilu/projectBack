package com.eshop.app.storage.infrastructure.r2;

import com.eshop.app.storage.domain.model.ImageDimensions;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class R2ImageMetadataExtractor {

    public ImageDimensions extractDimensions(byte[] bytes) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
            BufferedImage img = ImageIO.read(bais);
            if (img == null) {
                log.warn("R2 [DIMENSIONS]: ImageIO returned null — unrecognized format");
                return ImageDimensions.unknown();
            }
            return new ImageDimensions(img.getWidth(), img.getHeight());
        } catch (IOException e) {
            log.error("R2 [DIMENSIONS FAILED]: {}", e.getMessage());
            return ImageDimensions.unknown();
        }
    }
}
