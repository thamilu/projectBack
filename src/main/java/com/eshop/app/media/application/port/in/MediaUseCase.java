package com.eshop.app.media.application.port.in;

import com.eshop.app.media.api.request.ConfirmUploadRequest;
import com.eshop.app.media.api.request.RequestUploadUrlRequest;
import com.eshop.app.media.api.response.MediaAssetResponse;
import com.eshop.app.media.api.response.UploadUrlResponse;
import java.util.List;
import java.util.UUID;

/**
 * Use case interface for media operations.
 *
 * <p>Defines the application's media management contract. Implementations must handle security,
 * validation, and R2 integration.
 */
public interface MediaUseCase {

    /**
     * Generate a presigned upload URL for direct-to-R2 upload.
     *
     * @param request upload request with filename, content type, size
     * @param userId authenticated user ID from JWT
     * @return presigned URL and upload metadata
     */
    UploadUrlResponse requestUploadUrl(RequestUploadUrlRequest request, String userId);

    /**
     * Confirm a successful upload and activate the media asset.
     *
     * @param request confirmation with upload ID and image metadata
     * @param userId authenticated user ID from JWT
     * @return confirmed media asset details
     */
    MediaAssetResponse confirmUpload(ConfirmUploadRequest request, String userId);

    /**
     * Soft-delete a media asset and remove from R2 storage.
     *
     * @param assetId media asset UUID
     * @param userId authenticated user ID from JWT
     */
    void deleteMediaAsset(UUID assetId, String userId);

    /**
     * Set a media asset as the primary image for its product.
     *
     * @param productId product ID
     * @param assetId media asset UUID to set as primary
     * @param userId authenticated user ID from JWT
     * @return updated media asset with isPrimary = true
     */
    MediaAssetResponse setPrimaryImage(Long productId, UUID assetId, String userId);

    /**
     * Get all confirmed media assets for a product.
     *
     * @param productId product ID
     * @return list of media asset responses
     */
    List<MediaAssetResponse> getProductMedia(Long productId);
}
