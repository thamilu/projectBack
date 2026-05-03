package com.eshop.app.service;

import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.WishlistResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Wishlist service interface for managing user favorites
 * Provides fast add/remove operations and wishlist management
 * 
 * Time Complexity: All operations O(1) or O(log n)
 * Space Complexity: O(n) where n is number of wishlist items
 */
public interface WishlistService {
    
    /**
     * Add product to user's wishlist
     * 
     * @param userId User ID
     * @param productId Product ID to add
     * @param notes Optional notes about the product
     * @return Wishlist item response
     * 
     * Business Rules:
     * - Prevents duplicate entries (user + product combination unique)
     * - Product must exist and be active
     * - User must be authenticated
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
     * Get user's wishlist as simple list (for quick access)
     */
    List<WishlistResponse> getUserWishlistItems(Long userId);
    
    /**
     * Get wishlist with product details
     * Includes full product information for display
     */
    List<WishlistResponse> getUserWishlistWithDetails(Long userId);
    
    /**
     * Get wishlist items by store
     * Shows only items from specific store
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
     * Get most wishlisted products (popular products)
     */
    List<Object> getMostWishlistedProducts(int limit);
    
    /**
     * Get wishlist statistics by category
     */
    List<Object> getWishlistStatisticsByCategory();
    
    /**
     * Get users who wishlisted products from a store
     * Useful for marketing to interested customers
     */
    List<Object> getUsersInterestedInStore(Long storeId);
    
    /**
     * Update wishlist item notes
     */
    WishlistResponse updateWishlistNotes(Long userId, Long productId, String notes);
    
    /**
     * Move wishlist items to cart
     * Converts wishlist items to cart items for checkout
     */
    List<Object> moveWishlistToCart(Long userId, List<Long> productIds);
    
    /**
     * Get wishlist recommendations
     * Suggests products based on wishlist patterns
     */
    List<Object> getWishlistRecommendations(Long userId);
}