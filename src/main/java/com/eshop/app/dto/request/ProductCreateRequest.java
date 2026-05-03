package com.eshop.app.dto.request;

import com.eshop.app.entity.enums.CategoryType;
import com.eshop.app.validation.ValidPriceDiscount;
import com.eshop.app.validation.ValidationGroups;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidPriceDiscount
@Schema(description = "Request payload for creating a new product")
public class ProductCreateRequest {
    
    // ==================== BASIC INFORMATION ====================
    
    @Schema(
        description = "Product name displayed to customers",
        example = "iPhone 15 Pro 256GB",
        requiredMode = Schema.RequiredMode.REQUIRED,
        maxLength = 200
    )
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;
    
    @Schema(
        description = "Detailed product description (plain text)",
        example = "The latest iPhone with A17 Pro chip...",
        maxLength = 2000
    )
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @Schema(
        description = "Unique Stock Keeping Unit",
        example = "IPHONE-15-PRO-256",
        requiredMode = Schema.RequiredMode.REQUIRED,
        pattern = "^[A-Z0-9-]+$"
    )
    @NotBlank(message = "SKU is required", groups = ValidationGroups.Create.class)
    @Size(min = 3, max = 50, message = "SKU must be between 3 and 50 characters")
    @Pattern(
        regexp = "^[A-Z0-9][A-Z0-9-]{1,48}[A-Z0-9]$",
        message = "SKU must contain only uppercase letters, numbers, and hyphens"
    )
    private String sku;
    
    @Schema(
        description = "SEO-friendly URL slug",
        example = "iphone-15-pro-256gb",
        pattern = "^[a-z0-9-]+$"
    )
    @Size(max = 200, message = "Friendly URL must not exceed 200 characters")
    @Pattern(
        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
        message = "Friendly URL must contain only lowercase letters, numbers, and hyphens"
    )
    private String friendlyUrl;
    
    // ==================== PRICING ====================
    
    @Schema(
        description = "Regular selling price",
        example = "999.99",
        requiredMode = Schema.RequiredMode.REQUIRED,
        minimum = "0.01"
    )
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @DecimalMax(value = "99999999.99", message = "Price exceeds maximum allowed")
    @Digits(integer = 8, fraction = 2, message = "Price format is invalid (max 8 digits, 2 decimals)")
    private BigDecimal price;
    
    @Schema(
        description = "Discounted price (must be less than regular price)",
        example = "899.99",
        minimum = "0"
    )
    @DecimalMin(value = "0.00", message = "Discount price cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Discount price exceeds maximum allowed")
    @Digits(integer = 8, fraction = 2, message = "Discount price format is invalid")
    private BigDecimal discountPrice;
    
    // ==================== INVENTORY ====================
    
    @Schema(
        description = "Available stock quantity",
        example = "100",
        minimum = "0",
        defaultValue = "0"
    )
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 999999, message = "Stock quantity exceeds maximum allowed")
    @Builder.Default
    private Integer stockQuantity = 0;
    
    // ==================== MEDIA ====================
    
    @Schema(
        description = "Primary product image URL",
        example = "https://cdn.example.com/products/iphone-15.jpg"
    )
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @URL(message = "Image URL must be a valid URL")
    private String imageUrl;
    
    // ==================== CATEGORIZATION ====================
    
    @Schema(
        description = "Category ID for the product",
        example = "1",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Category ID is required")
    @Positive(message = "Category ID must be positive")
    private Long categoryId;
    
    @Schema(
        description = "Category type enumeration",
        example = "ELECTRONICS"
    )
    private CategoryType categoryType;
    
    @Schema(
        description = "Sub-category name",
        example = "Smartphones"
    )
    @Size(max = 100, message = "Sub-category must not exceed 100 characters")
    @Pattern(
        regexp = "^[a-zA-Z0-9\\s-]*$",
        message = "Sub-category contains invalid characters"
    )
    private String subCategory;
    
    @Schema(
        description = "Dynamic key-value attributes (e.g., {\"brand\": \"Apple\", \"color\": \"Blue\"})",
        example = "{\"brand\": \"Apple\", \"color\": \"Blue\", \"storage\": \"256GB\"}"
    )
    private Map<String, String> attributes;
    
    @Schema(
        description = "Brand ID",
        example = "1"
    )
    @Positive(message = "Brand ID must be positive")
    private Long brandId;
    
    // ==================== SHOP ====================
    
    @Schema(
        description = "Shop/Store ID (optional - auto-resolved from authenticated seller)",
        example = "1"
    )
    @Positive(message = "Store ID must be positive")
    private Long storeId;
    
    // ==================== TAGS & FEATURES ====================
    
    @Schema(
        description = "Product tags for search and filtering",
        example = "[\"smartphone\", \"apple\", \"5g\"]"
    )
    @Size(max = 20, message = "Maximum 20 tags allowed")
    @Builder.Default
    private Set<
        @NotBlank(message = "Tag cannot be blank")
        @Size(min = 2, max = 50, message = "Tag must be between 2 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Tag contains invalid characters")
        String
    > tags = new HashSet<>();

    // Return an immutable view to prevent accidental modification
    public Set<String> getTags() {
        return tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }
    
    @Schema(
        description = "Whether product should be featured on homepage",
        example = "false",
        defaultValue = "false"
    )
    @Builder.Default
    private Boolean featured = false;
    
    // ==================== CUSTOM VALIDATION ====================
    
    /**
     * Business validation that can't be done with annotations
     */
    @AssertTrue(message = "Discount price must be less than regular price")
    @Schema(hidden = true)
    private boolean isValidDiscountPrice() {
        if (discountPrice == null || price == null) {
            return true;
        }
        return discountPrice.compareTo(price) < 0;
    }
    
    @AssertTrue(message = "Discount price requires a regular price to be set")
    @Schema(hidden = true)
    private boolean isDiscountPriceValid() {
        if (discountPrice != null && price == null) {
            return false;
        }
        return true;
    }
}