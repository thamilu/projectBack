package com.eshop.app.media.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.media.api.request.ConfirmUploadRequest;
import com.eshop.app.media.api.request.RequestUploadUrlRequest;
import com.eshop.app.media.api.response.MediaAssetResponse;
import com.eshop.app.media.api.response.UploadUrlResponse;
import com.eshop.app.media.application.port.in.MediaUseCase;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Media management REST controller.
 *
 * <p>Provides enterprise-grade APIs for:
 *
 * <ul>
 *   <li>Generating presigned upload URLs for direct-to-R2 uploads
 *   <li>Confirming completed uploads
 *   <li>Managing product images (delete, set primary)
 *   <li>Retrieving product media assets
 * </ul>
 *
 * @author E-Shop Team
 */
@RestController
@RequestMapping(ApiConstants.Endpoints.MEDIA)
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media asset management APIs for product images")
public class MediaController {

    private final MediaUseCase mediaUseCase;

    // ==================== UPLOAD URL ====================

    @PostMapping("/upload-url")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    @Operation(
            summary = "Request presigned upload URL",
            description =
                    "Generates a presigned PUT URL for direct-to-R2 image upload. "
                            + "The URL expires after 15 minutes.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Upload URL generated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid request (bad MIME type, file too large)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "422",
                description = "Max images per product exceeded")
    })
    public ResponseEntity<ApiResponse<UploadUrlResponse>> requestUploadUrl(
            @RequestBody @Valid RequestUploadUrlRequest request, @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        UploadUrlResponse response = mediaUseCase.requestUploadUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ==================== CONFIRM UPLOAD ====================

    @PostMapping("/confirm")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    @Operation(
            summary = "Confirm upload completion",
            description =
                    "Confirms that the frontend has successfully uploaded "
                            + "to R2 using the presigned URL. Activates the media asset.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Upload confirmed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Upload ID not found or already confirmed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Not the owner of this upload")
    })
    public ResponseEntity<ApiResponse<MediaAssetResponse>> confirmUpload(
            @RequestBody @Valid ConfirmUploadRequest request, @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        MediaAssetResponse response = mediaUseCase.confirmUpload(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== DELETE ====================

    @DeleteMapping("/{id}")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    @Operation(
            summary = "Delete media asset",
            description = "Soft-deletes a media asset. The R2 object is cleaned up asynchronously.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "204",
                description = "Media asset deleted successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Media asset not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Not the owner of this asset")
    })
    public ResponseEntity<Void> deleteMediaAsset(
            @Parameter(description = "Media asset UUID") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        mediaUseCase.deleteMediaAsset(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== SET PRIMARY ====================

    @PatchMapping("/{id}/primary")
    @PreAuthorize(IS_ADMIN_OR_SELLER)
    @Operation(
            summary = "Set primary product image",
            description =
                    "Sets the specified media asset as the primary image for its product. "
                            + "Unsets the previous primary image.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Primary image updated successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Media asset not found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Asset is not active or not linked to a product"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Not the owner of this asset")
    })
    public ResponseEntity<ApiResponse<MediaAssetResponse>> setPrimaryImage(
            @Parameter(description = "Media asset UUID") @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        MediaAssetResponse response = mediaUseCase.setPrimaryImage(null, id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== GET PRODUCT MEDIA ====================

    @GetMapping("/product/{productId}")
    @Operation(
            summary = "Get product media assets",
            description =
                    "Returns all confirmed media assets for a product, "
                            + "ordered by primary flag then creation date.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Product media retrieved successfully")
    })
    public ResponseEntity<ApiResponse<List<MediaAssetResponse>>> getProductMedia(
            @Parameter(description = "Product ID") @PathVariable Long productId) {

        List<MediaAssetResponse> response = mediaUseCase.getProductMedia(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
