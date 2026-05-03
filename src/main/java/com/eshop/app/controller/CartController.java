package com.eshop.app.controller;

import com.eshop.app.dto.request.CartItemRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.CartResponse;
import com.eshop.app.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

/**
 * Cart Controller - Simple cart management for authenticated users
 * Handles basic cart operations with JWT authentication
 */
@Tag(name = "Carts", description = "Shopping cart management for authenticated users (CUSTOMER, ADMIN)")
@RestController
@RequestMapping(ApiConstants.Endpoints.CART)
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
public class CartController {
    
    private final CartService cartService;
    
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }
    
    @GetMapping
    @Operation(
        summary = "Get user's cart",
        description = "Retrieve the current user's shopping cart with all items and totals",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        CartResponse response = cartService.getCart();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PostMapping("/items")
    @Operation(
        summary = "Add item to cart",
        description = "Add a product to the shopping cart. If product already exists, quantity will be increased.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addItemToCart(request);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart", response));
    }
    
    @PutMapping("/items/{itemId}")
    @Operation(
        summary = "Update cart item quantity",
        description = "Update the quantity of a specific cart item",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @Parameter(description = "Cart Item ID") @PathVariable Long itemId,
            @Parameter(description = "New quantity") @RequestParam Integer quantity) {
        CartResponse response = cartService.updateCartItem(itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", response));
    }
    
    @DeleteMapping("/items/{itemId}")
    @Operation(
        summary = "Remove item from cart",
        description = "Remove a specific item from the shopping cart",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
            @Parameter(description = "Cart Item ID") @PathVariable Long itemId) {
        CartResponse response = cartService.removeItemFromCart(itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", response));
    }
    
    @DeleteMapping("/clear")
    @Operation(
        summary = "Clear entire cart",
        description = "Remove all items from the shopping cart",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> clearCart() {
        cartService.clearCart();
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
