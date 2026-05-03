package com.eshop.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductLocationSearchRequest {
    @Schema(description = "User latitude (WGS84)", example = "37.7749")
    @NotNull
    private Double latitude;

    @Schema(description = "User longitude (WGS84)", example = "-122.4194")
    @NotNull
    private Double longitude;

    @Schema(description = "Radius in kilometers (max 500)", example = "10.0")
    @Min(0)
    @Max(500)
    @Builder.Default
    private Double radiusKm = 10.0;

    @Schema(description = "Search keyword")
    private String keyword;

    private Long categoryId;
    private Long brandId;

    @Schema(description = "City filter", example = "San Francisco")
    private String city;

    @Schema(description = "State filter", example = "California")
    private String state;

    @Schema(description = "Country filter", example = "USA")
    private String country;

    @Schema(description = "Specific store ID filter", example = "1")
    private Long storeId;

    @Schema(description = "Only show in-stock products", example = "true")
    @Builder.Default
    private Boolean inStockOnly = false;

    @Schema(description = "Sort by: distance, price_asc, price_desc, rating, newest", example = "distance")
    @Builder.Default
    private String sortBy = "distance";

    @Schema(description = "Page number (0-indexed)", example = "0")
    @Min(0)
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Page size", example = "20")
    @Min(1)
    @Max(100)
    @Builder.Default
    private Integer size = 20;
}