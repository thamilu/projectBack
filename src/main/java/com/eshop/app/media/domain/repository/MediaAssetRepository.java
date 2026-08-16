package com.eshop.app.media.domain.repository;

import com.eshop.app.media.domain.entity.MediaAsset;
import com.eshop.app.media.domain.entity.UploadStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for media asset persistence operations.
 *
 * <p>All queries automatically filter soft-deleted records via {@code @SQLRestriction("deleted_at
 * IS NULL")} on the entity.
 */
@Repository
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    /**
     * Find all confirmed media assets for a product, ordered by primary flag then creation date.
     */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.productId = :productId
              AND m.uploadStatus = 'CONFIRMED'
            ORDER BY m.isPrimary DESC, m.createdAt ASC
            """)
    Page<MediaAsset> findByProductIdConfirmed(
            @Param("productId") Long productId, Pageable pageable);

    /** Find the primary image for a product. */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.productId = :productId
              AND m.isPrimary = true
              AND m.uploadStatus = 'CONFIRMED'
            """)
    Optional<MediaAsset> findPrimaryByProductId(@Param("productId") Long productId);

    /** Find a media asset by storage key. */
    Optional<MediaAsset> findByStorageKey(String storageKey);

    /**
     * Find orphaned assets: PENDING status older than the given cutoff time. Used by the orphan
     * cleanup scheduled job.
     */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.uploadStatus = :status
              AND m.createdAt < :cutoff
            """)
    List<MediaAsset> findByUploadStatusAndCreatedAtBefore(
            @Param("status") UploadStatus status, @Param("cutoff") LocalDateTime cutoff);

    /** Count confirmed media assets for a product. */
    @Query(
            """
            SELECT COUNT(m) FROM MediaAsset m
            WHERE m.productId = :productId
              AND m.uploadStatus = 'CONFIRMED'
            """)
    long countConfirmedByProductId(@Param("productId") Long productId);

    /** Unset primary flag for all assets of a product. Used before setting a new primary image. */
    @Modifying
    @Query(
            """
            UPDATE MediaAsset m
            SET m.isPrimary = false, m.updatedAt = CURRENT_TIMESTAMP
            WHERE m.productId = :productId AND m.isPrimary = true
            """)
    void unsetPrimaryForProduct(@Param("productId") Long productId);

    /** Find a confirmed asset by ID and uploader (ownership check). */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.id = :id
              AND m.uploadedBy = :uploadedBy
              AND m.uploadStatus = 'CONFIRMED'
            """)
    Optional<MediaAsset> findByIdAndUploadedByConfirmed(
            @Param("id") UUID id, @Param("uploadedBy") String uploadedBy);

    /** Find a pending asset by ID (for upload confirmation). */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.id = :id
              AND m.uploadStatus = 'PENDING'
            """)
    Optional<MediaAsset> findByIdAndPending(@Param("id") UUID id);

    /** Find all confirmed assets for a product (non-paginated, for internal use). */
    @Query(
            """
            SELECT m FROM MediaAsset m
            WHERE m.productId = :productId
              AND m.uploadStatus = 'CONFIRMED'
            ORDER BY m.isPrimary DESC, m.createdAt ASC
            """)
    List<MediaAsset> findAllConfirmedByProductId(@Param("productId") Long productId);
}
