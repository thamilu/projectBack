package com.eshop.app.core.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Request for batch delete operations with options.
 *
 * @param ids     the identifiers to delete; must be non-empty, contain no {@code null} or
 *                non-positive values, and not exceed {@value #MAX_BATCH_SIZE} items
 * @param options delete behavior options; when omitted ({@code null}), defaults to
 *                non-atomic mode ({@code atomicMode = false}) as a null-safety normalization,
 *                not a business rule
 */
public record BatchDeleteRequest(
    @NotEmpty(message = "ID set cannot be empty")
    @Size(max = BatchDeleteRequest.MAX_BATCH_SIZE, message = "Maximum {max} items per batch delete")
    Set<@NotNull @Positive Long> ids,

    DeleteOptions options
) {

    /** Maximum number of identifiers allowed in a single batch delete request. */
    private static final int MAX_BATCH_SIZE = 100;

    public BatchDeleteRequest {
        // Defensive copy to prevent post-construction mutation of the backing Set.
        // Null is intentionally preserved (not defaulted/rejected here) so that @NotEmpty
        // produces the intended validation message rather than a raw NullPointerException.
        // A null element inside a non-null set is likewise preserved (LinkedHashSet permits
        // one null) so the @NotNull element constraint reports a proper validation violation
        // instead of failing during the copy itself.
        ids = (ids == null) ? null : Collections.unmodifiableSet(new LinkedHashSet<>(ids));

        // Null-safety normalization only (not a business decision): guarantees options is
        // never null for downstream consumers of this DTO.
        options = (options == null) ? new DeleteOptions(false) : options;
    }

    /**
     * Delete operation options.
     *
     * @param atomicMode when {@code true}, the batch delete must succeed or fail as a whole;
     *                   when {@code false} (default), partial failures are permitted
     */
    public record DeleteOptions(
        boolean atomicMode
    ) {}
}
