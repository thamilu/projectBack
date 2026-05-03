package com.eshop.app.entity;

import com.eshop.app.entity.enums.ImageType;
import jakarta.persistence.*;
import lombok.*;

/**
 * Product image entity with support for multiple image types.
 */
@Entity
@Table(name = "product_images", indexes = {
    @Index(name = "idx_image_product", columnList = "product_id"),
    @Index(name = "idx_image_primary", columnList = "is_primary"),
    @Index(name = "idx_image_sort", columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "url", nullable = false, length = 1000)
    private String url;
    
    @Column(name = "alt_text", length = 255)
    private String altText;
    
    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "public_id", length = 500)
    private String publicId;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;
    
    /**
     * Renamed from displayOrder to match Product entity reference.
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
    
    /**
     * Image type (GALLERY, THUMBNAIL, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 20)
    @Builder.Default
    private ImageType imageType = ImageType.GALLERY;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    /**
     * Helper to check if this is the primary image.
     */
    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }

    // ==================== BACKWARD COMPATIBILITY ====================

    /**
     * @deprecated Use url field instead.
     */
    @Deprecated
    public String getImageUrl() {
        return url;
    }

    /**
     * @deprecated Use url field instead.
     */
    @Deprecated
    public void setImageUrl(String imageUrl) {
        this.url = imageUrl;
    }

    /**
     * @deprecated Use sortOrder field instead.
     */
    @Deprecated
    public Integer getDisplayOrder() {
        return sortOrder;
    }

    /**
     * @deprecated Use sortOrder field instead.
     */
    @Deprecated
    public void setDisplayOrder(Integer displayOrder) {
        this.sortOrder = displayOrder;
    }

    /**
     * Custom builder class to support deprecated field names.
     */
    public static class ProductImageBuilder {
        /**
         * @deprecated Use url() method instead.
         */
        @Deprecated
        public ProductImageBuilder imageUrl(String imageUrl) {
            this.url = imageUrl;
            return this;
        }

        /**
         * @deprecated Use sortOrder() method instead.
         */
        @Deprecated
        public ProductImageBuilder displayOrder(Integer displayOrder) {
            return sortOrder(displayOrder);
        }
    }
}
