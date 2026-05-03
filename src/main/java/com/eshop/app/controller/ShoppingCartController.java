package com.eshop.app.controller;

import com.eshop.app.dto.request.CartItemRequest;
import com.eshop.app.dto.request.CheckoutRequest;
import com.eshop.app.dto.request.MultipleCartItemsRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.CartResponse;
import com.eshop.app.dto.response.OrderResponse;
import com.eshop.app.service.CartService;
import com.eshop.app.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

/**
 * Shopping Cart Controller providing comprehensive cart management APIs.
 * 
 * Features:
 * - Anonymous Cart Management: Guest users can create and manage carts without authentication
 * - Authenticated Cart Operations: Logged-in users have persistent cart management
 * - Batch Operations: Support for adding multiple products simultaneously
 * - Checkout Integration: Direct cart-to-order conversion with validation
 * - Security: Role-based access control with JWT authentication
 * 
 * API Endpoints:
 * 
 * Anonymous Cart Operations:
 * - POST   /api/v1/cart                          # Create new anonymous cart
 * - GET    /api/v1/cart/{code}                   # Get cart by code
 * - PUT    /api/v1/cart/{code}                   # Update cart items
 * - DELETE /api/v1/cart/{code}                   # Clear entire cart
 * - POST   /api/v1/cart/{code}/items             # Add single product
 * - PUT    /api/v1/cart/{code}/items/batch       # Add multiple products
 * - DELETE /api/v1/cart/{code}/items/{productId} # Remove specific product
 * - POST   /api/v1/cart/{code}/checkout          # Anonymous checkout
 * 
 * Authenticated Cart Operations:
 * - GET    /auth/cart                           # Get user's cart
 * - POST   /auth/cart/{code}/checkout            # Authenticated checkout
 * 
 * Performance Optimizations:
 * - O(1) cart lookups using indexed cart codes
 * - Batch operations for multiple product additions
 * - Optimized database queries with proper relationships
 * 
 * Security Features:
 * - JWT token validation for authenticated operations
 * - Role-based access control (CUSTOMER, SELLER, ADMIN)
 * - Input validation using Bean Validation
 * - SQL injection prevention through JPA
 * 
 * Error Handling:
 * - Comprehensive exception handling with proper HTTP status codes
 * - Validation error responses with detailed messages
 * - Resource not found handling for invalid cart codes
 * 
 * @author EShop Development Team
 * @version 2.0
 * @since 2.0
 * Space Complexity: O(n) where n is the number of cart items
 */
@RestController
@RequestMapping(ApiConstants.Endpoints.SHOPPING_CART)
@Tag(name = "Shopping Cart", description = "Shopping cart management with anonymous and authenticated support")
public class ShoppingCartController {
    
    private final CartService cartService;
    private final OrderService orderService;
    
    public ShoppingCartController(CartService cartService, OrderService orderService) {
        this.cartService = cartService;
        this.orderService = orderService;
    }
    
    // Anonymous Cart Operations
    
    @PostMapping("/cart")
    @Operation(
        summary = "Create new anonymous cart",
        description = "Create a new shopping cart for anonymous users. Returns unique cart code."
    )
    public ResponseEntity<ApiResponse<CartResponse>> createAnonymousCart() {
        CartResponse response = cartService.createAnonymousCart();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Cart created successfully", response));
    }
    
    @GetMapping("/cart/{code}")
    @Operation(
        summary = "Get cart contents by code",
        description = "Retrieve cart contents using cart code (anonymous) or user authentication"
    )
    public ResponseEntity<ApiResponse<CartResponse>> getCartByCode(
            @Parameter(description = "Cart code") @PathVariable String code) {
        CartResponse response = cartService.getCartByCode(code);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @PutMapping("/cart/{code}")
    @Operation(
        summary = "Update entire cart",
        description = "Update cart with new items (replaces existing items)"
    )
    public ResponseEntity<ApiResponse<CartResponse>> updateCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Valid @RequestBody MultipleCartItemsRequest request) {
        CartResponse response = cartService.updateCart(code, request);
        return ResponseEntity.ok(ApiResponse.success("Cart updated successfully", response));
    }
    
    @PostMapping("/cart/{code}/product")
    @Operation(
        summary = "Add single product to cart",
        description = "Add a single product to cart by SKU"
    )
    public ResponseEntity<ApiResponse<CartResponse>> addProductToCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse response = cartService.addProductToCart(code, request);
        return ResponseEntity.ok(ApiResponse.success("Product added to cart", response));
    }
    
    @PostMapping("/cart/{code}/multi")
    @Operation(
        summary = "Add multiple products to cart",
        description = "Add multiple products to cart in a single operation"
    )
    public ResponseEntity<ApiResponse<CartResponse>> addMultipleProductsToCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Valid @RequestBody MultipleCartItemsRequest request) {
        CartResponse response = cartService.addMultipleProductsToCart(code, request);
        return ResponseEntity.ok(ApiResponse.success("Products added to cart", response));
    }
    
    @DeleteMapping("/cart/{code}/product/{sku}")
    @Operation(
        summary = "Remove product from cart",
        description = "Remove a product from cart by SKU"
    )
    public ResponseEntity<ApiResponse<CartResponse>> removeProductFromCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Parameter(description = "Product SKU") @PathVariable String sku) {
        CartResponse response = cartService.removeProductFromCart(code, sku);
        return ResponseEntity.ok(ApiResponse.success("Product removed from cart", response));
    }
    
    // Checkout Operations
    
    @PostMapping("/cart/{code}/checkout")
    @Operation(
        summary = "Checkout anonymous cart",
        description = "Convert anonymous cart to order without authentication"
    )
    public ResponseEntity<ApiResponse<OrderResponse>> checkoutAnonymousCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkoutAnonymousCart(code, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }
    
    @PostMapping("/auth/cart/{code}/checkout")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(
        summary = "Checkout authenticated cart",
        description = "Convert authenticated user's cart to order",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<OrderResponse>> checkoutAuthenticatedCart(
            @Parameter(description = "Cart code") @PathVariable String code,
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkoutAuthenticatedCart(code, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", response));
    }
    
    // Authenticated Cart Operations
    
    @PostMapping("/customers/{id}/cart")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create customer cart (Admin)",
        description = "Create a cart for a specific customer (Admin only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> createCustomerCart(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        CartResponse response = cartService.createCustomerCart(id);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer cart created successfully", response));
    }
    
    @GetMapping("/auth/customer/{id}/cart")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN') and (@userSecurity.isCurrentUser(#id) or hasRole('ADMIN'))")
    @Operation(
        summary = "Get customer cart",
        description = "Get cart for authenticated customer",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CartResponse>> getCustomerCart(
            @Parameter(description = "Customer ID") @PathVariable Long id) {
        CartResponse response = cartService.getCustomerCart(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}