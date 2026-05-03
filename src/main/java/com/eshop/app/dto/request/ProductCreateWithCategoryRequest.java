package com.eshop.app.dto.request;

import com.eshop.app.validation.ValidCategorySelection;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidCategorySelection  // Custom cross-field validation
public class ProductCreateWithCategoryRequest {

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_.]+$", 
             message = "Name contains invalid characters")
    private String name;

    @Size(max = 2000, message = "Description cannot exceed 2000 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @DecimalMax(value = "999999.99", message = "Price cannot exceed 999999.99")
    @Digits(integer = 6, fraction = 2, message = "Price must have max 6 integer and 2 decimal places")
    private BigDecimal price;

    @Min(value = 0, message = "Quantity cannot be negative")
    @Max(value = 100000, message = "Quantity cannot exceed 100000")
    @Builder.Default
    private Integer quantity = 0;

    @Positive(message = "Category ID must be positive")
    private Long categoryId;

    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-_]+$", 
             message = "Category name contains invalid characters")
    private String newCategoryName;

    @Size(max = 50, message = "SKU cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9\\-]*$", message = "SKU must be uppercase alphanumeric")
    private String sku;

    private java.util.Map<String, String> attributes;

    // Business logic helper
    public boolean hasExistingCategory() {
        return categoryId != null;
    }

    public boolean hasNewCategory() {
        return newCategoryName != null && !newCategoryName.isBlank();
    }
}