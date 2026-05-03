package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Detailed brand response with statistics")
public class BrandDetailResponse extends BrandResponse {

    @Schema(description = "Total number of products")
    private Long productCount;

    @Schema(description = "Number of active products")
    private Long activeProductCount;

    @Schema(description = "Average product price")
    private BigDecimal averageProductPrice;

    @Schema(description = "Minimum product price")
    private BigDecimal minProductPrice;

    @Schema(description = "Maximum product price")
    private BigDecimal maxProductPrice;

    @Schema(description = "Categories this brand appears in")
    private List<CategorySummaryResponse> categories;

    @Schema(description = "SEO meta title")
    private String metaTitle;

    @Schema(description = "SEO meta description")
    private String metaDescription;

    
}
