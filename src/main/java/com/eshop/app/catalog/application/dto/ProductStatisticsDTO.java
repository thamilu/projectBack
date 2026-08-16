package com.eshop.app.catalog.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Typed DTO carrying aggregated product statistics for a seller. Replaces the untyped Map returned
 * by repositories.
 */
@Getter
@Builder
public class ProductStatisticsDTO {
    private final Long totalProducts;
    private final Long activeProducts;
    private final Double averageRating;
}
