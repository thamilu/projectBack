package com.eshop.app.media.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Media asset entity for enterprise image management.
 *
 * <p>Decoupled from catalog module — linked to products via {@code product_id} FK but managed
 * independently through the media module.
 *
 * <p>Supports the signed upload URL flow:
 *
 * <ol>
 *   <li>Backend creates record with {@link UploadStatus#PENDING}
 *   <li>Frontend uploads directly to R2 using presigned URL
 *   <li>Frontend confirms upload → status becomes {@link UploadStatus#CONFIRMED}
 *   <li>Orphan cleanup job marks stale PENDING records as {@link UploadStatus#ORPHANED}
 * </ol>
 *
 * @author E-Shop Team
 * @version 1.0
 */
@Entity
@Table(
        name = "media_asset",
        indexes = {
            @Index(name = "idx_media_asset_product_id", columnList = "product_id"),
            @Index(name = "idx_media_asset_primary", columnList = "product_id, is_primary"),
            @Index(
                    name = "idx_media_asset_orphan_cleanup",
                    columnList = "upload_status, created_at"),
            @Index(name = "idx_media_asset_uploaded_by", columnList = "uploaded_by"),
            @Index(name = "idx_media_asset_created_at", columnList = "created_at")
        },
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_media_asset_storage_key", columnNames = "storage_key")
        })
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class MediaAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    /** Linked product ID. Nullable for assets uploaded before product creation. */
    @Column(name = "product_id")
    private Long productId;

    @NotBlank
    @Size(max = 255)
    @Column(name = "original_filename", nullable = false, length = 255)
    @ToString.Include
    private String originalFilename;

    /** R2 object key (e.g., "products/abc123/image.webp"). */
    @NotBlank
    @Size(max = 500)
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    /** Public CDN URL for the full-size image. */
    @Size(max = 1000)
    @Column(name = "cdn_url", length = 1000)
    private String cdnUrl;

    /** Public CDN URL for the thumbnail. */
    @Size(max = 1000)
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @NotBlank
    @Size(max = 100)
    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @NotNull
    @Column(name = "file_size", nullable = false)
    @Builder.Default
    private Long fileSize = 0L;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    @Builder.Default
    @ToString.Include
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    @NotBlank
    @Size(max = 255)
    @Column(name = "uploaded_by", nullable = false, length = 255)
    private String uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** Soft delete timestamp. Non-null means deleted. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ==================== DOMAIN BEHAVIOR ====================

    /** Confirm the upload with metadata from the frontend. */
    public void confirmUpload(
            String cdnUrl, String thumbnailUrl, Integer width, Integer height, Long fileSize) {
        this.cdnUrl = cdnUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.width = width;
        this.height = height;
        this.fileSize = fileSize;
        this.uploadStatus = UploadStatus.CONFIRMED;
    }

    /** Mark this asset as the primary image. */
    public void markAsPrimary() {
        this.isPrimary = true;
    }

    /** Unmark this asset as the primary image. */
    public void unmarkPrimary() {
        this.isPrimary = false;
    }

    /** Soft delete this asset. */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.uploadStatus = UploadStatus.DELETED;
    }

    /** Mark as orphaned by cleanup job. */
    public void markOrphaned() {
        this.uploadStatus = UploadStatus.ORPHANED;
    }

    /** Mark upload as failed. */
    public void markFailed() {
        this.uploadStatus = UploadStatus.FAILED;
    }

    /** Link this asset to a product. */
    public void linkToProduct(Long productId) {
        this.productId = productId;
    }

    /** Check if this asset is confirmed and active. */
    public boolean isActive() {
        return uploadStatus == UploadStatus.CONFIRMED && deletedAt == null;
    }

    /** Check if primary. */
    public boolean isPrimary() {
        return Boolean.TRUE.equals(isPrimary);
    }
}
