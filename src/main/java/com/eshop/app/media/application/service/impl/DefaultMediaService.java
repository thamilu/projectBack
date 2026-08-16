package com.eshop.app.media.application.service.impl;

import com.eshop.app.core.exception.security.UnauthorizedException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.media.api.request.ConfirmUploadRequest;
import com.eshop.app.media.api.request.RequestUploadUrlRequest;
import com.eshop.app.media.api.response.MediaAssetResponse;
import com.eshop.app.media.api.response.UploadUrlResponse;
import com.eshop.app.media.application.mapper.MediaAssetMapper;
import com.eshop.app.media.application.port.in.MediaUseCase;
import com.eshop.app.media.domain.entity.MediaAsset;
import com.eshop.app.media.domain.entity.UploadStatus;
import com.eshop.app.media.domain.repository.MediaAssetRepository;
import com.eshop.app.media.shared.constants.MediaConstants;
import com.eshop.app.media.shared.exception.MediaNotFoundException;
import com.eshop.app.media.shared.exception.MediaUploadException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * Enterprise media service implementing the signed upload URL flow.
 *
 * <p>Upload Flow:
 *
 * <ol>
 *   <li>{@code requestUploadUrl} — validates → creates PENDING asset → generates presigned URL
 *   <li>Frontend uploads directly to R2 using the presigned URL
 *   <li>{@code confirmUpload} — verifies ownership → marks CONFIRMED → builds CDN URLs
 * </ol>
 *
 * @author E-Shop Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DefaultMediaService implements MediaUseCase {

    private final MediaAssetRepository mediaAssetRepository;
    private final MediaAssetMapper mediaAssetMapper;
    private final ObjectProvider<S3Presigner> s3PresignerProvider;
    private final AppProperties appProperties;

    // ==================== UPLOAD URL GENERATION ====================

    @Override
    @Transactional
    public UploadUrlResponse requestUploadUrl(RequestUploadUrlRequest request, String userId) {
        if (s3PresignerProvider.getIfAvailable() == null) {
            throw new MediaUploadException(
                    "Cloudflare R2 storage provider is not active. Presigned upload URLs are only"
                            + " available in R2 mode.");
        }
        // 1. Validate MIME type
        if (!MediaConstants.ALLOWED_MIME_TYPES.contains(request.getContentType())) {
            throw new MediaUploadException(
                    "Invalid content type: "
                            + request.getContentType()
                            + ". Allowed: "
                            + MediaConstants.ALLOWED_MIME_TYPES);
        }

        // 2. Validate file size
        if (request.getFileSize() > MediaConstants.MAX_FILE_SIZE_BYTES) {
            throw new MediaUploadException(
                    "File size "
                            + request.getFileSize()
                            + " exceeds maximum of "
                            + MediaConstants.MAX_FILE_SIZE_BYTES
                            + " bytes");
        }

        // 3. Validate file extension
        String extension = FilenameUtils.getExtension(request.getFilename()).toLowerCase();
        if (!MediaConstants.ALLOWED_EXTENSIONS.contains(extension)) {
            throw new MediaUploadException(
                    "File extension '"
                            + extension
                            + "' not allowed. Allowed: "
                            + MediaConstants.ALLOWED_EXTENSIONS);
        }

        // 4. Check max images per product (if product specified)
        if (request.getProductId() != null) {
            long currentCount =
                    mediaAssetRepository.countConfirmedByProductId(request.getProductId());
            if (currentCount >= MediaConstants.MAX_IMAGES_PER_PRODUCT) {
                throw new MediaUploadException(
                        "Product already has "
                                + currentCount
                                + " images. Maximum allowed: "
                                + MediaConstants.MAX_IMAGES_PER_PRODUCT);
            }
        }

        // 5. Generate unique storage key
        String storageKey = generateStorageKey(request.getProductId(), extension);

        // 6. Create PENDING media asset record
        MediaAsset asset =
                MediaAsset.builder()
                        .productId(request.getProductId())
                        .originalFilename(request.getFilename())
                        .storageKey(storageKey)
                        .mimeType(request.getContentType())
                        .fileSize(request.getFileSize())
                        .uploadedBy(userId)
                        .uploadStatus(UploadStatus.PENDING)
                        .isPrimary(false)
                        .build();

        MediaAsset saved = mediaAssetRepository.save(asset);

        // 7. Generate presigned PUT URL
        Duration expiry = MediaConstants.DEFAULT_PRESIGNED_URL_EXPIRY;
        String presignedUrl = generatePresignedPutUrl(storageKey, request.getContentType(), expiry);

        log.info(
                "Upload URL generated. assetId={}, storageKey={}, user={}, productId={}",
                saved.getId(),
                storageKey,
                userId,
                request.getProductId());

        return UploadUrlResponse.builder()
                .uploadId(saved.getId())
                .uploadUrl(presignedUrl)
                .storageKey(storageKey)
                .expiresAt(LocalDateTime.now().plus(expiry))
                .build();
    }

    // ==================== UPLOAD CONFIRMATION ====================

    @Override
    @Transactional
    public MediaAssetResponse confirmUpload(ConfirmUploadRequest request, String userId) {
        // 1. Find pending asset
        MediaAsset asset =
                mediaAssetRepository
                        .findByIdAndPending(request.getUploadId())
                        .orElseThrow(
                                () -> new MediaNotFoundException(request.getUploadId().toString()));

        // 2. Verify ownership
        if (!asset.getUploadedBy().equals(userId)) {
            log.warn(
                    "Upload confirm denied. assetId={}, owner={}, requester={}",
                    asset.getId(),
                    asset.getUploadedBy(),
                    userId);
            throw new UnauthorizedException("You do not have permission to confirm this upload");
        }

        // 3. Build CDN URLs
        String publicUrl = buildCdnUrl(asset.getStorageKey());
        String thumbnailUrl = buildThumbnailUrl(asset.getStorageKey());

        // 4. Confirm the upload with metadata
        Long fileSize = request.getFileSize() != null ? request.getFileSize() : asset.getFileSize();
        asset.confirmUpload(
                publicUrl, thumbnailUrl, request.getWidth(), request.getHeight(), fileSize);

        // 5. Auto-set as primary if first image for product
        if (asset.getProductId() != null) {
            long confirmedCount =
                    mediaAssetRepository.countConfirmedByProductId(asset.getProductId());
            if (confirmedCount == 0) {
                asset.markAsPrimary();
                log.info(
                        "Auto-set as primary image. assetId={}, productId={}",
                        asset.getId(),
                        asset.getProductId());
            }
        }

        MediaAsset confirmed = mediaAssetRepository.save(asset);

        log.info(
                "Upload confirmed. assetId={}, storageKey={}, productId={}, size={}",
                confirmed.getId(),
                confirmed.getStorageKey(),
                confirmed.getProductId(),
                confirmed.getFileSize());

        return mediaAssetMapper.toResponse(confirmed);
    }

    // ==================== DELETE ====================

    @Override
    @Transactional
    public void deleteMediaAsset(UUID assetId, String userId) {
        MediaAsset asset =
                mediaAssetRepository
                        .findById(assetId)
                        .orElseThrow(() -> new MediaNotFoundException(assetId.toString()));

        // Ownership check
        if (!asset.getUploadedBy().equals(userId)) {
            log.warn(
                    "Delete denied. assetId={}, owner={}, requester={}",
                    assetId,
                    asset.getUploadedBy(),
                    userId);
            throw new UnauthorizedException(
                    "You do not have permission to delete this media asset");
        }

        // Soft delete
        asset.softDelete();
        mediaAssetRepository.save(asset);

        log.info(
                "Media asset soft-deleted. assetId={}, storageKey={}, user={}",
                assetId,
                asset.getStorageKey(),
                userId);
    }

    // ==================== SET PRIMARY ====================

    @Override
    @Transactional
    public MediaAssetResponse setPrimaryImage(Long productId, UUID assetId, String userId) {
        // 1. Find and verify the target asset
        MediaAsset asset =
                mediaAssetRepository
                        .findById(assetId)
                        .orElseThrow(() -> new MediaNotFoundException(assetId.toString()));

        if (!asset.isActive()) {
            throw new MediaUploadException(
                    "Cannot set non-active asset as primary. Status: " + asset.getUploadStatus());
        }

        // Derive productId from asset if not provided
        Long resolvedProductId = productId != null ? productId : asset.getProductId();

        if (resolvedProductId == null) {
            throw new MediaUploadException("Media asset is not linked to any product");
        }

        if (asset.getProductId() == null || !asset.getProductId().equals(resolvedProductId)) {
            throw new MediaUploadException(
                    "Media asset does not belong to product: " + resolvedProductId);
        }

        // 2. Ownership check
        if (!asset.getUploadedBy().equals(userId)) {
            throw new UnauthorizedException(
                    "You do not have permission to modify this media asset");
        }

        // 3. Unset current primary for this product
        mediaAssetRepository.unsetPrimaryForProduct(resolvedProductId);

        // 4. Set new primary
        asset.markAsPrimary();
        MediaAsset updated = mediaAssetRepository.save(asset);

        log.info(
                "Primary image updated. productId={}, assetId={}, user={}",
                resolvedProductId,
                assetId,
                userId);

        return mediaAssetMapper.toResponse(updated);
    }

    // ==================== READ ====================

    @Override
    public List<MediaAssetResponse> getProductMedia(Long productId) {
        List<MediaAsset> assets = mediaAssetRepository.findAllConfirmedByProductId(productId);
        return mediaAssetMapper.toResponseList(assets);
    }

    // ==================== PRIVATE HELPERS ====================

    /**
     * Generate a unique R2 storage key. Format: products/{productId}/{uuid}.{extension} or:
     * products/unlinked/{uuid}.{extension} for unlinked uploads.
     */
    private String generateStorageKey(Long productId, String extension) {
        String folder =
                productId != null
                        ? MediaConstants.PRODUCT_IMAGE_FOLDER + "/" + productId
                        : MediaConstants.PRODUCT_IMAGE_FOLDER + "/unlinked";
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        return folder + "/" + filename;
    }

    /** Generate a presigned PUT URL for direct upload to R2. */
    private String generatePresignedPutUrl(String storageKey, String contentType, Duration expiry) {
        AppProperties.Storage.R2 r2Config = appProperties.getStorage().getR2();

        PutObjectRequest objectRequest =
                PutObjectRequest.builder()
                        .bucket(r2Config.getBucketName())
                        .key(storageKey)
                        .contentType(contentType)
                        .cacheControl("public, max-age=31536000, immutable")
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(expiry)
                        .putObjectRequest(objectRequest)
                        .build();

        S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
        if (s3Presigner == null) {
            throw new IllegalStateException("S3Presigner is not configured");
        }
        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    /** Build the public CDN URL from the storage key. */
    private String buildCdnUrl(String storageKey) {
        String publicUrl = appProperties.getStorage().getR2().getPublicUrl();
        if (publicUrl == null || publicUrl.isBlank()) {
            return storageKey;
        }
        if (!publicUrl.endsWith("/")) {
            publicUrl += "/";
        }
        return publicUrl + storageKey;
    }

    /**
     * Build the thumbnail CDN URL from the storage key. Convention: original key with "thumb_"
     * prefix on the filename.
     */
    private String buildThumbnailUrl(String storageKey) {
        String publicUrl = appProperties.getStorage().getR2().getPublicUrl();
        if (publicUrl == null || publicUrl.isBlank()) {
            return storageKey;
        }
        if (!publicUrl.endsWith("/")) {
            publicUrl += "/";
        }
        // Convert "products/123/abc.webp" → "products/123/thumb_abc.webp"
        String thumbKey =
                storageKey.replaceFirst("([^/]+)$", MediaConstants.THUMBNAIL_PREFIX + "$1");
        return publicUrl + thumbKey;
    }
}
