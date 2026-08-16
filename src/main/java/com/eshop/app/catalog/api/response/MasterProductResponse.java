package com.eshop.app.catalog.api.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasterProductResponse {
    private Long id;
    private String name;
    private String slug;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private String baseDescription;
    private String shortDescription;
    private String specifications;
    private String imageUrl;
    private Long parentMasterProductId;
    private Long rootMasterProductId;
    private String productType;
    private String derivedFromSellerId;
    private String derivedReason;
    private Boolean createdFromCatalog;
    private List<MediaResponse> media;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaResponse {
        private Long id;
        private String mediaUrl;
        private String mediaType;
        private Boolean isPrimary;
        private Integer sortOrder;
        private String altText;
    }
}
