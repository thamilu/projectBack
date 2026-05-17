package com.eshop.app.catalog.api.response;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product search criteria")
public class ProductSearchCriteria {

    @Size(max = 100)
    private String keyword;

    private Long categoryId;

    private Long brandId;

    private Long storeId;

    @DecimalMin("0.0")
    private BigDecimal minPrice;

    @DecimalMax("9999999.99")
    private BigDecimal maxPrice;

    private Set<String> tags;

    private Boolean featured;

    private Boolean inStock;

    private Boolean active;
    private Set<Long> categoryIds;
    private Set<Long> brandIds;
    private Boolean hasDiscount;
    private String createdAfter;
    private String createdBefore;
    private Long sellerId;

}
