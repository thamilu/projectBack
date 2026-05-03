package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Wishlist entity for user's favorite products
 * Optimized for fast lookups and efficient storage
 * 
 * Time Complexity: O(1) for add/remove operations
 * Space Complexity: O(n) where n is number of wishlist items per user
 */
@Entity
@Table(name = "wishlists", indexes = {
    @Index(name = "idx_wishlist_user_id", columnList = "user_id"),
    @Index(name = "idx_wishlist_product_id", columnList = "product_id"),
    @Index(name = "idx_wishlist_user_product", columnList = "user_id,product_id", unique = true),
    @Index(name = "idx_wishlist_created_at", columnList = "created_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "notes", length = 500)
    private String notes; // Optional user notes about the product
    
    /**
     * Create wishlist item for user and product
     */
    public static Wishlist create(User user, Product product) {
        return Wishlist.builder()
                .user(user)
                .product(product)
                .build();
    }
    
    /**
     * Create wishlist item with notes
     */
    public static Wishlist create(User user, Product product, String notes) {
        return Wishlist.builder()
                .user(user)
                .product(product)
                .notes(notes)
                .build();
    }
}