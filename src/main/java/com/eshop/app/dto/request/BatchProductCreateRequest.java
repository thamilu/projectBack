package com.eshop.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request wrapper for batch product creation with options.
 *
 * @author E-Shop Team
 * @since 2.0.0
 */
public record BatchProductCreateRequest(
    @NotEmpty(message = "Product list cannot be empty")
    @Size(max = 100, message = "Maximum 100 products per batch")
    List<@Valid ProductCreateRequest> products,
    
    BatchOptions options
) {
    public BatchProductCreateRequest {
        if (options == null) {
            options = new BatchOptions(false, false);
        }
    }
    
    /**
     * Batch processing options
     */
    public record BatchOptions(
        boolean stopOnError,
        boolean validateOnly
    ) {}
}
