package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Brand summary for dropdowns")
public class BrandSummaryResponse {

    @Schema(description = "Brand ID", example = "1")
    private Long id;

    @Schema(description = "Brand name", example = "Apple")
    private String name;

    @Schema(description = "Brand slug", example = "apple")
    private String slug;

    @Schema(description = "Brand logo URL")
    private String logoUrl;
}
