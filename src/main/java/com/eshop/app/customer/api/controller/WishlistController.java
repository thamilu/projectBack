package com.eshop.app.customer.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.customer.api.request.WishlistAddRequest;
import com.eshop.app.customer.api.response.*;
import com.eshop.app.customer.application.port.in.WishlistService;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.util.SecurityUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Wishlist Controller for managing user favorites.
 * Part of the Customer module.
 */
@Tag(name = "Wishlist Management", description = "User favorites and wishlist operations")
@RestController
@RequestMapping({ApiConstants.BASE_PATH + "/wishlist", ApiConstants.Endpoints.CART + "/wishlist"})
@RequiredArgsConstructor
@Validated
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class WishlistController {

    private final WishlistService wishlistService;

    @Deprecated
    @PostMapping("/add")
    @Operation(summary = "Add to Wishlist (Legacy)", description = "Add product to user's wishlist. Deprecated: use POST /items instead.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @Parameter(description = "User ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "Product ID") @RequestParam @NotNull @Positive Long productId,
            @Parameter(description = "Optional notes") @RequestParam(required = false) @Size(max = 500) String notes) {
        WishlistResponse response = wishlistService.addToWishlist(userId, productId, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", response));
    }

    @Deprecated
    @DeleteMapping("/remove")
    @Operation(summary = "Remove from Wishlist (Legacy)", description = "Remove product from user's wishlist. Deprecated: use DELETE /items/{productId} instead.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @Parameter(description = "User ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "Product ID") @RequestParam @NotNull @Positive Long productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @Deprecated
    @GetMapping("/check")
    @Operation(summary = "Check if in Wishlist (Legacy)", description = "Check if product is in user's wishlist. Deprecated: use GET /check/{productId} instead.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @Parameter(description = "User ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "Product ID") @RequestParam @NotNull @Positive Long productId) {
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }

    @Deprecated
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Wishlist (Legacy)", description = "Get paginated wishlist for user. Deprecated: use GET / instead.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getUserWishlist(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<WishlistResponse> wishlist = wishlistService.getUserWishlist(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @Deprecated
    @GetMapping("/user/{userId}/items")
    @Operation(summary = "Get User Wishlist Items (Legacy)", description = "Get simple list of user's wishlist items.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistItems(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistItems(userId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @Deprecated
    @GetMapping("/user/{userId}/detailed")
    @Operation(summary = "Get User Wishlist with Details (Legacy)", description = "Get wishlist with full product information.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistWithDetails(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId) {
        List<WishlistResponse> wishlist = wishlistService.getUserWishlistWithDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @GetMapping("/user/{userId}/store/{storeId}")
    @Operation(summary = "Get Wishlist by Store", description = "Get user's wishlist items from specific store")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistByStore(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId,
            @Parameter(description = "Store ID") @PathVariable @NotNull @Positive Long storeId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistByStore(userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/user/{userId}/category/{categoryId}")
    @Operation(summary = "Get Wishlist by Category", description = "Get user's wishlist items from specific category")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistByCategory(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId,
            @Parameter(description = "Category ID") @PathVariable @NotNull @Positive Long categoryId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistByCategory(userId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/user/{userId}/search")
    @Operation(summary = "Search User Wishlist", description = "Search user's wishlist by product name")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> searchUserWishlist(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId,
            @Parameter(description = "Search keyword") @RequestParam @NotBlank @Size(max = 100) String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<WishlistResponse> results = wishlistService.searchUserWishlist(userId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get Wishlist Count", description = "Get total number of items in user's wishlist")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId) {
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/user/{userId}/clear")
    @Operation(summary = "Clear Wishlist", description = "Remove all items from user's wishlist")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<Void> clearWishlist(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId) {
        wishlistService.clearWishlist(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    @Operation(summary = "Get Most Wishlisted Products", description = "Get most popular products based on wishlist count")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<ApiResponse<List<PopularWishlistResponse>>> getMostWishlistedProducts(
            @Parameter(description = "Limit number of results") @RequestParam(defaultValue = "10") @Positive @Max(100) int limit) {
        List<Object> products = wishlistService.getMostWishlistedProducts(limit);
        List<PopularWishlistResponse> mapped = products.stream()
                .map(obj -> {
                    Object[] arr = (Object[]) obj;
                    return PopularWishlistResponse.builder()
                            .productId((Long) arr[0])
                            .productName("Product ID: " + arr[0])
                            .wishlistCount((Long) arr[1])
                            .build();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    @GetMapping("/statistics/category")
    @Operation(summary = "Get Wishlist Statistics by Category", description = "Get wishlist statistics grouped by category")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<ApiResponse<List<WishlistStatisticsResponse>>> getWishlistStatisticsByCategory() {
        List<Object> statistics = wishlistService.getWishlistStatisticsByCategory();
        List<WishlistStatisticsResponse> mapped = statistics.stream()
                .map(obj -> {
                    Object[] arr = (Object[]) obj;
                    return WishlistStatisticsResponse.builder()
                            .categoryId(null)
                            .categoryName((String) arr[0])
                            .wishlistItemCount((Long) arr[1])
                            .build();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    @GetMapping("/store/{storeId}/interested-users")
    @Operation(summary = "Get Users Interested in Store", description = "Get users who have wishlisted products from this store")
    @PreAuthorize("hasRole(@appProperties.security.roles.seller) or hasRole(@appProperties.security.roles.admin)")
    public ResponseEntity<ApiResponse<List<StoreInterestedUsersResponse>>> getUsersInterestedInStore(
            @Parameter(description = "Store ID") @PathVariable @NotNull @Positive Long storeId) {
        List<StoreInterestedUsersResponse> mapped = wishlistService.getUsersInterestedInStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    @Deprecated
    @PutMapping("/update-notes")
    @Operation(summary = "Update Wishlist Notes (Legacy)", description = "Update notes for a wishlist item. Deprecated: client supplied userId is insecure.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<WishlistResponse>> updateWishlistNotes(
            @Parameter(description = "User ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "Product ID") @RequestParam @NotNull @Positive Long productId,
            @Parameter(description = "New notes") @RequestParam @Size(max = 500) String notes) {
        WishlistResponse response = wishlistService.updateWishlistNotes(userId, productId, notes);
        return ResponseEntity.ok(ApiResponse.success("Wishlist notes updated", response));
    }

    @Deprecated
    @PostMapping("/move-to-cart")
    @Operation(summary = "Move Wishlist to Cart (Legacy)", description = "Move selected wishlist items to shopping cart. Deprecated: client supplied userId is insecure.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<Object>>> moveWishlistToCart(
            @Parameter(description = "User ID") @RequestParam @NotNull @Positive Long userId,
            @Parameter(description = "Product IDs to move") @RequestParam List<@NotNull @Positive Long> productIds) {
        List<Object> cartItems = wishlistService.moveWishlistToCart(userId, productIds);
        return ResponseEntity.ok(ApiResponse.success(cartItems));
    }

    @Deprecated
    @GetMapping("/user/{userId}/recommendations")
    @Operation(summary = "Get Wishlist Recommendations (Legacy)", description = "Get product recommendations based on wishlist. Deprecated: client supplied userId is insecure.")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or (hasRole(@appProperties.security.roles.customer) and #userId == principal.id)")
    public ResponseEntity<ApiResponse<List<WishlistRecommendationResponse>>> getWishlistRecommendations(
            @Parameter(description = "User ID") @PathVariable @NotNull @Positive Long userId) {
        List<Object> recommendations = wishlistService.getWishlistRecommendations(userId);
        List<WishlistRecommendationResponse> mapped = recommendations.stream()
                .map(obj -> {
                    if (obj instanceof WishlistRecommendationResponse) {
                        return (WishlistRecommendationResponse) obj;
                    }
                    return new WishlistRecommendationResponse();
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(mapped));
    }

    // ==================== RESTFUL FRONTEND DIRECT OPERATIONS ====================

    @GetMapping
    @Operation(summary = "Get Authenticated User Wishlist", description = "Get paginated wishlist for the authenticated user")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer)")
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getMyWishlist(
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        PageResponse<WishlistResponse> wishlist = wishlistService.getUserWishlist(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @PostMapping("/items")
    @Operation(summary = "Add to Wishlist (Body)", description = "Add product to authenticated user's wishlist using request body")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer)")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlistFromBody(
            @Valid @RequestBody WishlistAddRequest request) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        WishlistResponse response = wishlistService.addToWishlist(userId, request.getProductId(), null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", response));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove from Wishlist by Path", description = "Remove product from authenticated user's wishlist using path variable")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer)")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlistByPath(
            @PathVariable @NotNull @Positive Long productId) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @GetMapping("/check/{productId}")
    @Operation(summary = "Check if in Wishlist by Path", description = "Check if product is in authenticated user's wishlist using path variable")
    @PreAuthorize("hasRole(@appProperties.security.roles.customer)")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlistByPath(
            @PathVariable @NotNull @Positive Long productId) {
        Long userId = SecurityUtils.getAuthenticatedUserId();
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }
}


