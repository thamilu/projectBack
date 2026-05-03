package com.eshop.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Brand search criteria")
public class BrandSearchCriteria {

    @Schema(description = "Search keyword for name/description")
    @Size(max = 100, message = "Keyword must not exceed 100 characters")
    private String keyword;

    @Schema(description = "Filter by active status")
    private Boolean active;

    @Schema(description = "Filter by featured status")
    private Boolean featured;

    @Schema(description = "Filter by country")
    @Size(max = 100)
    private String country;

    @Schema(description = "Filter by category IDs")
    private Set<Long> categoryIds;

    @Schema(description = "Minimum product count")
    private Integer minProductCount;

    @Schema(description = "Established year from")
    private Integer establishedYearFrom;

    @Schema(description = "Established year to")
    private Integer establishedYearTo;
}
