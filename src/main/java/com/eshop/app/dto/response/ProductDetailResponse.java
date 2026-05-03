package com.eshop.app.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive product DTO for detailed product views.
 * 
 * <p>Includes all product information with related entities.
 * Use this for:
 * <ul>
 *   <li>Product detail pages</li>
 *   <li>Admin product management</li>
 *   <li>Full product export</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Comprehensive product details with all related data")
public class ProductDetailResponse {
    
    // ==================== CORE INFORMATION ====================
    
    @Schema(description = "Product ID", example = "1")
    private Long id;
    
    @Schema(description = "Product name", example = "iPhone 15 Pro Max 256GB", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Full product description", example = "Latest iPhone with titanium design...")
    private String description;
    
    @Schema(description = "Product SKU", example = "AAPL-IP15PM-256")
    private String sku;
    
    @Schema(description = "SEO-friendly URL", example = "iphone-15-pro-max-256gb")
    private String friendlyUrl;
    
    @Schema(description = "Main product image URL")
    private String imageUrl;
    
    // ==================== PRICING ====================
    
    @Schema(description = "Regular price", example = "1199.99")
    private BigDecimal price;
    
    @Schema(description = "Discounted price", example = "1099.99")
    private BigDecimal discountPrice;
    
    @Schema(description = "Effective price (discount or regular)", example = "1099.99")
    private BigDecimal effectivePrice;
    
    @Schema(description = "Discount percentage", example = "8.33")
    private BigDecimal discountPercentage;
    
    @Schema(description = "Whether product has active discount", example = "true")
    private Boolean hasDiscount;
    
    // ==================== INVENTORY ====================
    
    @Schema(description = "Current stock quantity", example = "50")
    private Integer stockQuantity;
    
    @Schema(description = "Whether product is in stock", example = "true")
    private Boolean inStock;
    
    @Schema(description = "Whether product can be purchased", example = "true")
    private Boolean isPurchasable;
    
    // ==================== FLAGS ====================
    
    @Schema(description = "Whether product is active", example = "true")
    private Boolean active;
    
    @Schema(description = "Whether product is featured", example = "false")
    private Boolean featured;
    
    @Schema(description = "Whether product is master variant", example = "true")
    private Boolean isMaster;
    
    // ==================== RELATIONSHIPS ====================
    
    @Schema(description = "Category ID", example = "5")
    private Long categoryId;
    
    @Schema(description = "Category name", example = "Smartphones")
    private String categoryName;
    
    @Schema(description = "Category type", example = "ELECTRONICS")
    private String categoryType;
    
    @Schema(description = "Sub-category", example = "Premium Phones")
    private String subCategory;
    
    @Schema(description = "Brand ID", example = "3")
    private Long brandId;
    
    @Schema(description = "Brand name", example = "Apple")
    private String brandName;
    
    @Schema(description = "Shop ID", example = "1")
    private Long shopId;
    
    @Schema(description = "Shop name", example = "Tech Store")
    private String storeName;
    
    @Schema(description = "Tax class ID")
    private Long taxClassId;
    
    // ==================== COLLECTIONS ====================
    
    @Schema(description = "Product tags", example = "[\"5G\", \"Premium\", \"New\"]")
    private Set<String> tags;
    
    @Schema(description = "Product attributes as key-value pairs")
    private Map<String, String> attributes;
    
    @Schema(description = "Category-specific attributes")
    private Map<String, String> categoryAttributes;
    
    // ==================== REVIEWS & RATINGS ====================
    
    @Schema(description = "Average rating", example = "4.5")
    private Double averageRating;
    
    @Schema(description = "Total number of reviews", example = "127")
    private Long reviewCount;
    
    @Schema(description = "List of recent reviews")
    private List<ProductReviewResponse> recentReviews;
    
    // ==================== AUDIT INFORMATION ====================
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
    
    @Schema(description = "Created by user")
    private String createdBy;
    
    @Schema(description = "Last updated by user")
    private String updatedBy;
    
    @Schema(description = "Entity version for optimistic locking", example = "1")
    private Long version;
    
    // ==================== EXTENDED INFORMATION ====================
    
    @Schema(description = "Base product information")
    private BaseInfoDto baseInfo;
    
    @Schema(description = "Pricing details")
    private PricingDto pricing;
    
    @Schema(description = "Inventory details")
    private InventoryDto inventory;
    
    @Schema(description = "Location-based pricing")
    private List<LocationPricingDto> locationBasedPricing;
    
    @Schema(description = "Availability details")
    private AvailabilityDto availability;
    
    @Schema(description = "Shipping restrictions")
    private ShippingRestrictionsDto shippingRestrictions;
}
