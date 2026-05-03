package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Category summary for brand details")
public class CategorySummaryResponse {
    private Long id;
    private String name;
    private String slug;
    private Long productCount;
}
