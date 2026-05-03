package com.eshop.app.dto.request;

import com.eshop.app.entity.enums.CategoryType;

import java.util.Map;
import com.eshop.app.validation.AtLeastOneNotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO for updating an existing product.
 * All fields are OPTIONAL - only provided fields will be updated.
 * At least one field must be provided for a valid update request.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@AtLeastOneNotNull(message = "At least one field must be provided for update")
@Schema(description = "Product update request - all fields optional, only provided fields are updated")
public class ProductUpdateRequest extends BaseProductRequest {

    // Note: All fields inherited from BaseProductRequest are OPTIONAL
    // No @NotNull or @NotBlank annotations - validate only if value provided

    // ==================== BASIC INFORMATION ====================

    @Schema(
        description = "Product name (optional for update)",
        example = "iPhone 15 Pro Max 512GB"
    )
    @Size(min = 2, max = 200, message = "Product name must be between 2 and 200 characters")
    private String name;

    @Schema(
        description = "Product description",
        example = "Updated product description..."
    )
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Schema(
        description = "SEO-friendly URL slug",
        example = "iphone-15-pro-max-512gb"
    )
    @Size(max = 200, message = "Friendly URL must not exceed 200 characters")
    @Pattern(
        regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
        message = "Friendly URL must contain only lowercase letters, numbers, and hyphens"
    )
    private String friendlyUrl;

    @Schema(
        description = "Short description for listings",
        example = "iPhone 15 Pro Max - 512GB"
    )
    @Size(max = 500, message = "Short description must not exceed 500 characters")
    private String shortDescription;

    // ==================== PRICING ====================

    @Schema(
        description = "Regular selling price",
        example = "1199.99"
    )
    @DecimalMin(value = "0.01", message = "Price must be at least 0.01")
    @DecimalMax(value = "99999999.99", message = "Price exceeds maximum")
    @Digits(integer = 8, fraction = 2, message = "Price format invalid")
    private BigDecimal price;

    @Schema(
        description = "Discounted price",
        example = "1099.99"
    )
    @DecimalMin(value = "0.00", message = "Discount price cannot be negative")
    @DecimalMax(value = "99999999.99", message = "Discount price exceeds maximum")
    @Digits(integer = 8, fraction = 2, message = "Discount price format invalid")
    private BigDecimal discountPrice;

    @Schema(
        description = "Original price before any discount",
        example = "1299.99"
    )
    @DecimalMin(value = "0.00", message = "Original price cannot be negative")
    private BigDecimal originalPrice;

    // ==================== INVENTORY ====================

    @Schema(
        description = "Available stock quantity",
        example = "50"
    )
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Max(value = 999999, message = "Stock quantity exceeds maximum")
    private Integer stockQuantity;

    @Schema(
        description = "Minimum order quantity",
        example = "1"
    )
    @Min(value = 1, message = "Minimum order quantity must be at least 1")
    private Integer minOrderQuantity;

    @Schema(
        description = "Maximum order quantity",
        example = "10"
    )
    @Positive(message = "Maximum order quantity must be positive")
    private Integer maxOrderQuantity;

    @Schema(
        description = "Low stock threshold for alerts",
        example = "10"
    )
    @PositiveOrZero(message = "Low stock threshold cannot be negative")
    private Integer lowStockThreshold;

    // ==================== MEDIA ====================

    @Schema(
        description = "Primary product image URL",
        example = "https://cdn.example.com/products/new-image.jpg"
    )
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    @org.hibernate.validator.constraints.URL(message = "Invalid image URL format")
    private String imageUrl;

    @Schema(
        description = "Additional image URLs",
        example = "[\"https://cdn.example.com/img1.jpg\", \"https://cdn.example.com/img2.jpg\"]"
    )
    @Size(max = 10, message = "Maximum 10 additional images allowed")
    private Set<
        @org.hibernate.validator.constraints.URL(message = "Invalid image URL")
        @Size(max = 500, message = "Image URL too long")
        String
    > additionalImages;

    // ==================== CATEGORIZATION ====================

    @Schema(
        description = "Category ID",
        example = "2"
    )
    @Positive(message = "Category ID must be positive")
    private Long categoryId;

    @Schema(description = "Category type")
    private CategoryType categoryType;

    @Schema(
        description = "Sub-category name",
        example = "Premium Smartphones"
    )
    @Size(max = 100, message = "Sub-category must not exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s-]*$", message = "Sub-category contains invalid characters")
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

    // ==================== TAGS ====================

    @Schema(
        description = "Product tags (replaces existing tags)",
        example = "[\"smartphone\", \"apple\", \"premium\"]"
    )
    @Size(max = 20, message = "Maximum 20 tags allowed")
    private Set<
        @NotBlank(message = "Tag cannot be blank")
        @Size(min = 2, max = 50, message = "Tag must be between 2 and 50 characters")
        @Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Tag contains invalid characters")
        String
    > tags;

    // ==================== FLAGS ====================

    @Schema(
        description = "Whether product is featured on homepage",
        example = "true"
    )
    private Boolean featured;

    @Schema(
        description = "Whether product is active/visible",
        example = "true"
    )
    private Boolean active;

    @Schema(
        description = "Whether product is available for sale",
        example = "true"
    )
    private Boolean available;

    // ==================== SEO ====================

    @Schema(
        description = "SEO meta title",
        example = "Buy iPhone 15 Pro Max 512GB | Best Price"
    )
    @Size(max = 100, message = "Meta title must not exceed 100 characters")
    private String metaTitle;

    @Schema(
        description = "SEO meta description",
        example = "Get the new iPhone 15 Pro Max with 512GB storage..."
    )
    @Size(max = 300, message = "Meta description must not exceed 300 characters")
    private String metaDescription;

    @Schema(
        description = "SEO meta keywords",
        example = "iphone, apple, smartphone, 5g"
    )
    @Size(max = 200, message = "Meta keywords must not exceed 200 characters")
    private String metaKeywords;

    // ==================== PHYSICAL ATTRIBUTES ====================

    @Schema(
        description = "Product weight in kg",
        example = "0.221"
    )
    @DecimalMin(value = "0.0", message = "Weight cannot be negative")
    private BigDecimal weight;

    @Schema(
        description = "Product dimensions",
        example = "159.9 x 76.7 x 8.25 mm"
    )
    @Size(max = 100, message = "Dimensions must not exceed 100 characters")
    private String dimensions;

    // ==================== CROSS-FIELD VALIDATION ====================

    /**
     * Validates discount price is less than regular price if both provided.
     */
    @AssertTrue(message = "Discount price must be less than regular price")
    @Schema(hidden = true)
    public boolean isValidDiscountPrice() {
        if (discountPrice == null || price == null) {
            return true; // Skip if either is not provided
        }
        return discountPrice.compareTo(price) < 0;
    }

    /**
     * Validates max order quantity is greater than min if both provided.
     */
    @AssertTrue(message = "Maximum order quantity must be greater than minimum")
    @Schema(hidden = true)
    public boolean isValidOrderQuantityRange() {
        if (minOrderQuantity == null || maxOrderQuantity == null) {
            return true;
        }
        return maxOrderQuantity > minOrderQuantity;
    }

    /**
     * Check if any field is set (for at least one field validation).
     */
    @Schema(hidden = true)
    public boolean hasAnyFieldSet() {
        return name != null || 
               description != null ||
               friendlyUrl != null ||
               price != null ||
               discountPrice != null ||
               stockQuantity != null ||
               imageUrl != null ||
               categoryId != null ||
               brandId != null ||
               tags != null ||
               featured != null ||
               active != null ||
               metaTitle != null ||
               metaDescription != null;
    }
}