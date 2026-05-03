package com.eshop.app.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Request for batch delete operations with options.
 *
 * @author E-Shop Team
 * @since 2.0.0
 */
public record BatchDeleteRequest(
    @NotEmpty(message = "ID set cannot be empty")
    @Size(max = 100, message = "Maximum 100 items per batch delete")
    Set<@Positive Long> ids,
    
    DeleteOptions options
) {
    public BatchDeleteRequest {
        if (options == null) {
            options = new DeleteOptions(false);
        }
    }
    
    /**
     * Delete operation options
     */
    public record DeleteOptions(
        boolean atomicMode
    ) {}
}
