package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Cart entity representing both authenticated user carts and anonymous guest carts.
 * 
 * Features:
 * - Anonymous Cart Support: Guest users can create carts using unique cart codes
 * - User-linked Carts: Authenticated users have persistent carts
 * - Automatic Total Calculation: Real-time cart total computation
 * - Orphan Removal: Automatic cleanup of cart items when cart is deleted
 * 
 * Database Optimizations:
 * - Indexed cart_code for O(1) anonymous cart lookups
 * - Indexed user_id for fast user cart retrieval
 * - Unique constraints on cart_code and user_id
 * 
 * Usage Patterns:
 * - Anonymous: Cart created with UUID-based cart_code, user=null
 * - Authenticated: Cart linked to user, cart_code for session management
 * 
 * @author EShop Development Team
 * @version 2.0
 * @since 1.0
 */
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_cart_user", columnList = "user_id"),
    @Index(name = "idx_cart_code", columnList = "cart_code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {
    
    /**
     * Unique cart identifier for anonymous cart access.
     * Generated using UUID format for security and uniqueness.
     * Used by both anonymous and authenticated users for cart operations.
     * 
     * Length: 50 characters maximum
     * Format: UUID string (e.g., "550e8400-e29b-41d4-a716-446655440000")
     * Index: Unique index for O(1) cart lookups
     */
    @Column(name = "cart_code", unique = true, length = 50)
    private String cartCode;
    
    /**
     * Associated user for authenticated carts.
     * Null for anonymous guest carts.
     * One-to-one relationship ensuring one active cart per user.
     */
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;
    
    /**
     * Collection of cart items with cascade operations.
     * Supports batch operations for performance optimization.
     * Orphan removal ensures automatic cleanup of deleted items.
     * 
     * Time Complexity: O(1) for add/remove, O(n) for calculations
     * Space Complexity: O(n) where n is number of items
     */
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private final Set<CartItem> items = new HashSet<>();
    
    /**
     * Calculated total amount for all items in cart.
     * Automatically updated when items are modified.
     * Precision: 10 digits with 2 decimal places for currency.
     */
    @Column(name = "total_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    /**
     * Calculates and updates the total amount for all cart items.
     * Should be called after any cart item modifications.
     * 
     * Time Complexity: O(n) where n is number of cart items
     * Space Complexity: O(1)
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
