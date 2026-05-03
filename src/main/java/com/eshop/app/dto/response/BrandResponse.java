package com.eshop.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Brand response")
public class BrandResponse {

    @Schema(description = "Brand ID", example = "1")
    private Long id;

    @Schema(description = "Brand name", example = "Apple")
    private String name;

    @Schema(description = "Brand description")
    private String description;

    @Schema(description = "URL-friendly slug", example = "apple")
    private String slug;

    @Schema(description = "Brand logo URL")
    private String logoUrl;

    @Schema(description = "Brand website URL")
    private String websiteUrl;

    @Schema(description = "Country of origin")
    private String country;

    @Schema(description = "Year established")
    private Integer establishedYear;

    @Schema(description = "Active status")
    private Boolean active;

    @Schema(description = "Featured status")
    private Boolean featured;

    @Schema(description = "Display order")
    private Integer displayOrder;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;

    @Schema(description = "SEO meta title")
    private String metaTitle;

    @Schema(description = "SEO meta description")
    private String metaDescription;
}
