package com.eshop.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request for atomic stock updates with operation type.
 *
 * @author E-Shop Team
 * @since 2.0.0
 */
public record StockUpdateRequest(
    @NotNull(message = "Operation type is required")
    StockOperation operation,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity
) {}
