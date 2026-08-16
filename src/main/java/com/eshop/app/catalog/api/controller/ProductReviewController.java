package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.request.ProductReviewRequest;
import com.eshop.app.catalog.api.response.ProductReviewResponse;
import com.eshop.app.catalog.application.port.in.ProductReviewUseCase;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "Product Reviews", description = "Product review management endpoints")
@RestController
@RequestMapping(ApiConstants.Endpoints.PRODUCT_REVIEWS)
public class ProductReviewController {

    private final ProductReviewUseCase reviewService;

    public ProductReviewController(ProductReviewUseCase reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    @Operation(summary = "Create product review", description = "Create a new review for a product (CUSTOMER and ADMIN only). Users can only review products they have purchased.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @Valid @RequestBody ProductReviewRequest request) {
        ProductReviewResponse response = reviewService.createReview(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", response));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    @Operation(summary = "Update product review", description = "Update an existing review (users can only update their own reviews, admins can update any)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId,
            @Valid @RequestBody ProductReviewRequest request) {
        ProductReviewResponse response = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    @Operation(summary = "Delete product review", description = "Delete a review (users can only delete their own reviews, admins can delete any)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @Parameter(description = "Review ID") @PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }

    @GetMapping("/{reviewId}")
    @Operation(summary = "Get review by ID", description = "Retrieve a specific review by its ID")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> getReviewById(
            @Parameter(description = "Review ID") @PathVariable Long reviewId) {
        ProductReviewResponse response = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product reviews", description = "Get all reviews for a specific product with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewResponse>>> getProductReviews(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<ProductReviewResponse> response = reviewService.getReviewsByProduct(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize(IS_ADMIN_OR_SELF_BY_USER_ID)
    @Operation(summary = "Get user reviews", description = "Get all reviews by a specific user (users can only see their own reviews, admins can see any)", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewResponse>>> getUserReviews(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<ProductReviewResponse> response = reviewService.getReviewsByUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-reviews")
    @PreAuthorize(IS_ADMIN_OR_CUSTOMER)
    @Operation(summary = "Get current user reviews", description = "Get all reviews by the currently authenticated user", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewResponse>>> getCurrentUserReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort.Direction direction = sortDir.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<ProductReviewResponse> response = reviewService.getCurrentUserReviews(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}


