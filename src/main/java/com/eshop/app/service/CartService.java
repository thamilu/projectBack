package com.eshop.app.service;

import com.eshop.app.dto.request.CartItemRequest;
import com.eshop.app.dto.request.MultipleCartItemsRequest;
import com.eshop.app.dto.response.CartResponse;

/**
 * Cart Service Interface providing comprehensive cart management functionality.
 * 
 * Features:
 * - Anonymous Cart Management: Support for guest user shopping carts
 * - Authenticated Cart Operations: Persistent carts for logged-in users
 * - Batch Operations: Efficient multiple product management
 * - Code-based Access: UUID-based cart identification for security
 * 
 * Performance Characteristics:
 * - Cart Creation: O(1) with UUID generation
 * - Cart Retrieval: O(1) with indexed cart codes
 * - Product Addition: O(1) for single items, O(n) for batch operations
 * - Cart Updates: O(1) for individual items
 * 
 * Security Features:
 * - UUID-based cart codes for anonymous access
 * - User validation for authenticated operations
 * - Input sanitization and validation
 * 
 * @author EShop Development Team
 * @version 2.0
 * @since 1.0
 */
public interface CartService {
    
    // Legacy authenticated cart methods
    
    /**
     * Retrieves the current authenticated user's cart.
     * @return CartResponse with user's cart details
     */
    CartResponse getCart();

    /**
     * Adds an item to the authenticated user's cart.
     * @param request Cart item details
     * @return Updated cart response
     */
    CartResponse addItemToCart(CartItemRequest request);
    
    /**
     * Updates quantity of an existing cart item.
     * @param itemId Cart item identifier
     * @param quantity New quantity
     * @return Updated cart response
     */
    CartResponse updateCartItem(Long itemId, Integer quantity);
    
    /**
     * Removes an item from the authenticated user's cart.
     * @param itemId Cart item identifier
     * @return Updated cart response
     */
    CartResponse removeItemFromCart(Long itemId);
    
    /**
     * Clears all items from the authenticated user's cart.
     */
    void clearCart();
    
    // Enhanced anonymous cart methods
    
    /**
     * Creates a new anonymous cart with unique cart code.
     * Generates UUID-based cart identifier for guest user access.
     * 
     * @return CartResponse with generated cart code and empty items
     * @throws CartCreationException if cart creation fails
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    CartResponse createAnonymousCart();

    CartResponse getCartByCode(String cartCode);

    CartResponse updateCart(String cartCode, MultipleCartItemsRequest request);

    CartResponse addProductToCart(String cartCode, CartItemRequest request);

    CartResponse addMultipleProductsToCart(String cartCode, MultipleCartItemsRequest request);

    CartResponse removeProductFromCart(String cartCode, String sku);

    CartResponse createCustomerCart(Long customerId);

    CartResponse getCustomerCart(Long customerId);
}
