package com.eshop.app.catalog.domain.entity;

import com.eshop.app.core.kernel.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/** Seller product image entity representing customized, listing-specific override images. */
@Entity
@Table(
        name = "seller_product_images",
        indexes = {
            @Index(name = "idx_seller_image_product", columnList = "product_id"),
            @Index(name = "idx_seller_image_deleted", columnList = "deleted")
        })
@SQLDelete(
        sql =
                "UPDATE seller_product_images SET deleted = true, deleted_at = CURRENT_TIMESTAMP,"
                    + " deleted_by = (SELECT current_user()), version = version + 1 WHERE id = ?"
                    + " AND version = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "storage_key", nullable = false, columnDefinition = "TEXT")
    private String storageKey;

    @Column(name = "alt_text", length = 500)
    private String altText;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 50)
    @Builder.Default
    private ImageType imageType = ImageType.GALLERY;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    // ==================== SOFT DELETE ====================

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;
}
