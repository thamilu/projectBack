package com.eshop.app.customer.application.port.in;

import com.eshop.app.customer.api.response.WishlistResponse;
import com.eshop.app.core.api.response.PageResponse;

import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Wishlist service interface for managing user favorites.
 * Acts as an Inbound Port for the Customer module.
 */
public interface WishlistService {

    /**
     * Add product to user's wishlist
     */
    WishlistResponse addToWishlist(Long userId, Long productId, String notes);

    /**
     * Remove product from user's wishlist
     */
    void removeFromWishlist(Long userId, Long productId);

    /**
     * Check if product is in user's wishlist
     */
    boolean isInWishlist(Long userId, Long productId);

    /**
     * Get user's wishlist with pagination
     */
    PageResponse<WishlistResponse> getUserWishlist(Long userId, Pageable pageable);

    /**
     * Get user's wishlist as simple list
     */
    List<WishlistResponse> getUserWishlistItems(Long userId);

    /**
     * Get wishlist with product details
     */
    List<WishlistResponse> getUserWishlistWithDetails(Long userId);

    /**
     * Get wishlist items by store
     */
    List<WishlistResponse> getUserWishlistByStore(Long userId, Long storeId);

    /**
     * Get wishlist items by category
     */
    List<WishlistResponse> getUserWishlistByCategory(Long userId, Long categoryId);

    /**
     * Search user's wishlist by product name
     */
    PageResponse<WishlistResponse> searchUserWishlist(Long userId, String keyword, Pageable pageable);

    /**
     * Get wishlist count for user
     */
    long getWishlistCount(Long userId);

    /**
     * Clear user's entire wishlist
     */
    void clearWishlist(Long userId);

    /**
     * Get most wishlisted products
     */
    List<Object> getMostWishlistedProducts(int limit);

    /**
     * Get wishlist statistics by category
     */
    List<Object> getWishlistStatisticsByCategory();

    /**
     * Get users who wishlisted products from a store
     */
    List<Object> getUsersInterestedInStore(Long storeId);

    /**
     * Update wishlist item notes
     */
    WishlistResponse updateWishlistNotes(Long userId, Long productId, String notes);

    /**
     * Move wishlist items to cart
     */
    List<Object> moveWishlistToCart(Long userId, List<Long> productIds);

    /**
     * Get wishlist recommendations
     */
    List<Object> getWishlistRecommendations(Long userId);
}


