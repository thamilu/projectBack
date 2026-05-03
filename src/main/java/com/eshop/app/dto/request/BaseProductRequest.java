package com.eshop.app.dto.request;

import com.eshop.app.validation.ValidPriceDiscount;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ValidPriceDiscount
@Schema(description = "Base product request fields")
public abstract class BaseProductRequest {

    @Size(min = 2, max = 200)
    private String name;

    @Size(max = 2000)
    private String description;

    @Size(max = 200)
    private String friendlyUrl;

    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal price;

    @DecimalMin(value = "0.00", inclusive = true)
    private BigDecimal discountPrice;

    @Min(0)
    private Integer stockQuantity;

    @Size(max = 500)
    private String imageUrl;

    @Positive
    private Long categoryId;

    @Positive
    private Long brandId;

    @Size(max = 20)
    private Set<String> tags;

    private Boolean featured;

    private Boolean active;
}
