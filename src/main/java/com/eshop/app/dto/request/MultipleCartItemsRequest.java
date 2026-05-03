package com.eshop.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for adding multiple products to cart in a single batch operation.
 * 
 * Purpose:
 * - Enables efficient bulk cart operations
 * - Reduces API call overhead for multiple product additions
 * - Supports shopping cart import/sync operations
 * - Optimizes database transactions with batch processing
 * 
 * Use Cases:
 * - Adding multiple products from wishlist to cart
 * - Importing cart from external source
 * - Bulk product addition during promotional campaigns
 * - Cart synchronization across devices
 * 
 * Validation Rules:
 * - Must contain at least one cart item
 * - Each item must pass individual validation
 * - Maximum batch size enforced for performance
 * - Product IDs must be valid and exist
 * - Quantities must be positive integers
 * 
 * Performance Characteristics:
 * - Single database transaction for all items
 * - Batch validation for improved efficiency
 * - Atomic operation (all succeed or all fail)
 * 
 * Error Handling:
 * - Returns validation errors for all invalid items
 * - Partial success not allowed (transaction rollback)
 * - Clear error messages for each validation failure
 * 
 * @author EShop Development Team
 * @version 1.0
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultipleCartItemsRequest {
    
    /**
     * List of cart items to be added to the cart in batch.
     * 
     * Constraints:
     * - Cannot be null or empty
     * - Each item must pass @Valid validation
     * - Recommended maximum: 50 items per batch
     * - Minimum: 1 item required
     * 
     * Validation Applied:
     * - NotEmpty: Ensures at least one item is provided
     * - Valid: Cascades validation to each CartItemRequest
     * - Product existence validation
     * - Stock availability check
     * - Quantity range validation (1-999)
     * 
     * Performance Impact:
     * - Larger batches may impact response time
     * - Database transaction size increases with item count
     * - Memory usage scales with batch size
     * 
     * Example:
     * [
     *   {"productId": 1, "quantity": 2},
     *   {"productId": 5, "quantity": 1},
     *   {"productId": 10, "quantity": 3}
     * ]
     */
    @NotEmpty(message = "Cart items list cannot be empty")
    @Valid
    private List<CartItemRequest> items;
}