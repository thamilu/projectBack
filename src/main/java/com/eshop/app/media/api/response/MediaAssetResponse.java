package com.eshop.app.media.api.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a media asset.
 *
 * <p>Used in all media read operations — never expose the entity directly.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaAssetResponse {

    private UUID id;
    private Long productId;
    private String originalFilename;
    private String cdnUrl;
    private String thumbnailUrl;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSize;
    private Boolean isPrimary;
    private String uploadStatus;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
}
