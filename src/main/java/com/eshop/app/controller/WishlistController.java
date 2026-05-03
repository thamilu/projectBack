package com.eshop.app.controller;

import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.WishlistResponse;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

import java.util.List;

/**
 * Wishlist Controller for managing user favorites
 * Provides fast add/remove operations and wishlist management
 * 
 * Security: User-specific access with ownership validation
 * Performance: O(1) operations with optimized queries
 */
@Tag(name = "Wishlist Management", description = "User favorites and wishlist operations")
@RestController
@RequestMapping(ApiConstants.Endpoints.CART + "/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "Keycloak OAuth2")
@SecurityRequirement(name = "Bearer Authentication")
public class WishlistController {

    private final WishlistService wishlistService;

    @PostMapping("/add")
    @Operation(summary = "Add to Wishlist", description = "Add product to user's wishlist")
    // Removed invalid Swagger @ApiResponse annotations
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<WishlistResponse>> addToWishlist(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product ID") @RequestParam Long productId,
            @Parameter(description = "Optional notes") @RequestParam(required = false) String notes) {
        WishlistResponse response = wishlistService.addToWishlist(userId, productId, notes);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product added to wishlist", response));
    }

    @DeleteMapping("/remove")
    @Operation(summary = "Remove from Wishlist", description = "Remove product from user's wishlist")
    // Removed invalid Swagger @ApiResponse annotation
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product ID") @RequestParam Long productId) {
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Product removed from wishlist", null));
    }

    @GetMapping("/check")
    @Operation(summary = "Check if in Wishlist", description = "Check if product is in user's wishlist")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product ID") @RequestParam Long productId) {
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(inWishlist));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get User Wishlist", description = "Get paginated wishlist for user")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> getUserWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<WishlistResponse> wishlist = wishlistService.getUserWishlist(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @GetMapping("/user/{userId}/items")
    @Operation(summary = "Get User Wishlist Items", description = "Get simple list of user's wishlist items")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistItems(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistItems(userId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/user/{userId}/detailed")
    @Operation(summary = "Get User Wishlist with Details", description = "Get wishlist with full product information")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistWithDetails(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<WishlistResponse> wishlist = wishlistService.getUserWishlistWithDetails(userId);
        return ResponseEntity.ok(ApiResponse.success(wishlist));
    }

    @GetMapping("/user/{userId}/store/{storeId}")
    @Operation(summary = "Get Wishlist by Store", description = "Get user's wishlist items from specific store")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistByStore(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Store ID") @PathVariable Long storeId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistByStore(userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/user/{userId}/category/{categoryId}")
    @Operation(summary = "Get Wishlist by Category", description = "Get user's wishlist items from specific category")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getUserWishlistByCategory(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Category ID") @PathVariable Long categoryId) {
        List<WishlistResponse> items = wishlistService.getUserWishlistByCategory(userId, categoryId);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @GetMapping("/user/{userId}/search")
    @Operation(summary = "Search User Wishlist", description = "Search user's wishlist by product name")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<WishlistResponse>>> searchUserWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId,
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<WishlistResponse> results = wishlistService.searchUserWishlist(userId, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get Wishlist Count", description = "Get total number of items in user's wishlist")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getWishlistCount(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        long count = wishlistService.getWishlistCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @DeleteMapping("/user/{userId}/clear")
    @Operation(summary = "Clear Wishlist", description = "Remove all items from user's wishlist")
    // Removed invalid Swagger @ApiResponse annotation
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> clearWishlist(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        wishlistService.clearWishlist(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    @Operation(summary = "Get Most Wishlisted Products", description = "Get most popular products based on wishlist count")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getMostWishlistedProducts(
            @Parameter(description = "Limit number of results") @RequestParam(defaultValue = "10") int limit) {
        List<Object> products = wishlistService.getMostWishlistedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/statistics/category")
    @Operation(summary = "Get Wishlist Statistics by Category", description = "Get wishlist statistics grouped by category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getWishlistStatisticsByCategory() {
        List<Object> statistics = wishlistService.getWishlistStatisticsByCategory();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }

    @GetMapping("/store/{storeId}/interested-users")
    @Operation(summary = "Get Users Interested in Store", description = "Get users who have wishlisted products from this store")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Object>>> getUsersInterestedInStore(
            @Parameter(description = "Store ID") @PathVariable Long storeId) {
        List<Object> users = wishlistService.getUsersInterestedInStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @PutMapping("/update-notes")
    @Operation(summary = "Update Wishlist Notes", description = "Update notes for a wishlist item")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<WishlistResponse>> updateWishlistNotes(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product ID") @RequestParam Long productId,
            @Parameter(description = "New notes") @RequestParam String notes) {
        WishlistResponse response = wishlistService.updateWishlistNotes(userId, productId, notes);
        return ResponseEntity.ok(ApiResponse.success("Wishlist notes updated", response));
    }

    @PostMapping("/move-to-cart")
    @Operation(summary = "Move Wishlist to Cart", description = "Move selected wishlist items to shopping cart")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Object>>> moveWishlistToCart(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Product IDs to move") @RequestParam List<Long> productIds) {
        List<Object> cartItems = wishlistService.moveWishlistToCart(userId, productIds);
        return ResponseEntity.ok(ApiResponse.success(cartItems));
    }

    @GetMapping("/user/{userId}/recommendations")
    @Operation(summary = "Get Wishlist Recommendations", description = "Get product recommendations based on wishlist")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<Object>>> getWishlistRecommendations(
            @Parameter(description = "User ID") @PathVariable Long userId) {
        List<Object> recommendations = wishlistService.getWishlistRecommendations(userId);
        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}