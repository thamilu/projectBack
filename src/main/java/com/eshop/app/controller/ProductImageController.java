package com.eshop.app.controller;

import com.eshop.app.dto.request.ProductImageRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.ProductImageResponse;
import com.eshop.app.service.ProductImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Product Images", description = "Product image management endpoints")
@RestController
@RequestMapping(ApiConstants.Endpoints.PRODUCT_IMAGES)
public class ProductImageController {
    
    private final ProductImageService imageService;
    
    public ProductImageController(ProductImageService imageService) {
        this.imageService = imageService;
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Add product image",
        description = "Upload a new image for a product (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ProductImageResponse>> addProductImage(
            @Valid @RequestBody ProductImageRequest request) {
        ProductImageResponse response = imageService.addProductImage(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product image added successfully", response));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadImage(
            @RequestParam("productId") Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "isPrimary", required = false, defaultValue = "false") boolean isPrimary
    ) {
        ProductImageResponse response = imageService.uploadProductImage(productId, file, altText, isPrimary);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Product image uploaded", response));
    }
    
    @PutMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Update product image",
        description = "Update an existing product image (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ProductImageResponse>> updateProductImage(
            @Parameter(description = "Image ID") @PathVariable Long imageId,
            @Valid @RequestBody ProductImageRequest request) {
        ProductImageResponse response = imageService.updateProductImage(imageId, request);
        return ResponseEntity.ok(ApiResponse.success("Product image updated successfully", response));
    }
    
    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Delete product image",
        description = "Delete a product image (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        imageService.deleteProductImage(imageId);
        return ResponseEntity.ok(ApiResponse.success("Product image deleted successfully", null));
    }
    
    @GetMapping("/{imageId}")
    @Operation(
        summary = "Get product image",
        description = "Retrieve a specific product image by its ID"
    )
    public ResponseEntity<ApiResponse<ProductImageResponse>> getProductImage(
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        ProductImageResponse response = imageService.getProductImageById(imageId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/product/{productId}")
    @Operation(
        summary = "Get product images",
        description = "Get all images for a specific product"
    )
    public ResponseEntity<ApiResponse<List<ProductImageResponse>>> getProductImages(
            @Parameter(description = "Product ID") @PathVariable Long productId) {
        List<ProductImageResponse> response = imageService.getProductImages(productId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/product/{productId}/primary/{imageId}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Set primary image",
        description = "Set an image as the primary image for a product (SELLER and ADMIN only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<ProductImageResponse>> setPrimaryImage(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Image ID") @PathVariable Long imageId) {
        ProductImageResponse response = imageService.setPrimaryImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Primary image set successfully", response));
    }
}