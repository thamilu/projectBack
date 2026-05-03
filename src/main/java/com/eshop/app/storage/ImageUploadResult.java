package com.eshop.app.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResult {
    private String publicId;
    private String url;
    private String thumbnailUrl;
    private Integer width;
    private Integer height;
    private Long fileSize;
}
