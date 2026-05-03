package com.eshop.app.repository;

import com.eshop.app.entity.Wishlist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Wishlist repository with optimized queries for user favorites
 * All queries are O(1) or O(log n) with proper indexing
 */
@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    
    /**
     * Find wishlist items by user - O(log n) with index
     */
    Page<Wishlist> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find wishlist items by user ordered by creation date
     */
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /**
     * Find specific wishlist item by user and product - O(1) with unique composite index
     */
    Optional<Wishlist> findByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Check if product is in user's wishlist - O(1) with unique composite index
     */
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Count wishlist items for user
     */
    long countByUserId(Long userId);
    
    /**
     * Find wishlist items by product (to see how many users wishlisted it)
     */
    Page<Wishlist> findByProductId(Long productId, Pageable pageable);
    
    /**
     * Count how many users wishlisted a product
     */
    long countByProductId(Long productId);
    
    /**
     * Find wishlist items with detailed product information
     */
    @Query("SELECT w FROM Wishlist w JOIN FETCH w.product p JOIN FETCH p.store WHERE w.user.id = :userId")
    List<Wishlist> findByUserIdWithProductDetails(@Param("userId") Long userId);
    
    /**
     * Find wishlist items for products from a specific store
     */
    @Query("SELECT w FROM Wishlist w JOIN w.product p WHERE w.user.id = :userId AND p.store.id = :storeId")
    List<Wishlist> findByUserIdAndStoreId(@Param("userId") Long userId, @Param("storeId") Long storeId);
    
    /**
     * Find wishlist items for products from a specific category
     */
    @Query("SELECT w FROM Wishlist w JOIN w.product p WHERE w.user.id = :userId AND p.category.id = :categoryId")
    List<Wishlist> findByUserIdAndCategoryId(@Param("userId") Long userId, @Param("categoryId") Long categoryId);
    
    /**
     * Get most wishlisted products
     */
    @Query("SELECT w.product.id, COUNT(w) as wishlist_count FROM Wishlist w " +
           "GROUP BY w.product.id ORDER BY wishlist_count DESC")
    List<Object[]> getMostWishlistedProducts(Pageable pageable);
    
    /**
     * Get wishlist statistics by category
     */
    @Query("SELECT p.category.name, COUNT(w) FROM Wishlist w JOIN w.product p " +
           "GROUP BY p.category.id, p.category.name ORDER BY COUNT(w) DESC")
    List<Object[]> getWishlistStatisticsByCategory();
    
    /**
     * Find users who wishlisted products from a specific store
     */
    @Query("SELECT DISTINCT w.user FROM Wishlist w JOIN w.product p WHERE p.store.id = :storeId")
    List<Object> findUsersWhoWishlistedFromStore(@Param("storeId") Long storeId);
    
    /**
     * Delete wishlist item by user and product
     */
    void deleteByUserIdAndProductId(Long userId, Long productId);
    
    /**
     * Delete all wishlist items for a user
     */
    void deleteByUserId(Long userId);
    
    /**
     * Search wishlist items by product name
     */
    @Query("SELECT w FROM Wishlist w JOIN w.product p " +
           "WHERE w.user.id = :userId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Wishlist> searchWishlistByProductName(@Param("userId") Long userId, 
                                              @Param("keyword") String keyword, 
                                              Pageable pageable);
}