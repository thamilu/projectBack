package com.eshop.app.entity;

import com.eshop.app.entity.enums.*;
import com.eshop.app.exception.InsufficientStockException;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.validator.constraints.URL;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Product entity representing items in the e-commerce catalog.
 * 
 * <h2>Features:</h2>
 * <ul>
 * <li>Master/variant product hierarchy with parent-child relationships</li>
 * <li>Soft delete with automatic timestamp tracking</li>
 * <li>Optimistic locking for concurrent update handling</li>
 * <li>Complete audit trail with user and timestamp tracking</li>
 * <li>Dynamic attributes for flexible product specifications</li>
 * <li>Multi-warehouse inventory support</li>
 * <li>SEO optimization fields</li>
 * <li>Scheduled pricing and visibility</li>
 * <li>Digital product support</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 3.0
 * @since 2024
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku"),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_brand", columnList = "brand_id"),
        @Index(name = "idx_product_store", columnList = "store_id"),
        @Index(name = "idx_product_status", columnList = "status, deleted"),
        @Index(name = "idx_product_featured", columnList = "featured, status"),
        @Index(name = "idx_product_price", columnList = "price"),
        @Index(name = "idx_product_created", columnList = "created_at"),
        @Index(name = "idx_product_category_status", columnList = "category_id, status, deleted"),
        @Index(name = "idx_product_parent", columnList = "parent_product_id"),
        @Index(name = "idx_product_friendly_url", columnList = "friendly_url"),
        @Index(name = "idx_product_visibility", columnList = "visible_from, visible_to, status"),
        @Index(name = "idx_product_type", columnList = "product_type")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_sku", columnNames = "sku"),
        @UniqueConstraint(name = "uk_product_friendly_url", columnNames = "friendly_url")
})
@SQLDelete(sql = "UPDATE products SET deleted = true, deleted_at = CURRENT_TIMESTAMP, " +
        "deleted_by = (SELECT current_user()), version = version + 1 WHERE id = ? AND version = ?")
@SQLRestriction("deleted = false")
@EntityListeners(AuditingEntityListener.class)
@NamedEntityGraphs({
        @NamedEntityGraph(name = "Product.summary", attributeNodes = {
                @NamedAttributeNode("category"),
                @NamedAttributeNode("brand")
        }),
        @NamedEntityGraph(name = "Product.withBasicRelations", attributeNodes = {
                @NamedAttributeNode("category"),
                @NamedAttributeNode("brand"),
                @NamedAttributeNode("store"),
                @NamedAttributeNode("taxClass"),
                @NamedAttributeNode("primaryImage")
        }),
        @NamedEntityGraph(name = "Product.withAllRelations", attributeNodes = {
                @NamedAttributeNode("category"),
                @NamedAttributeNode("brand"),
                @NamedAttributeNode("store"),
                @NamedAttributeNode("taxClass"),
                @NamedAttributeNode("tags"),
                @NamedAttributeNode("images"),
                @NamedAttributeNode(value = "variants", subgraph = "variant-subgraph")
        }, subgraphs = {
                @NamedSubgraph(name = "variant-subgraph", attributeNodes = {
                        @NamedAttributeNode("images")
                })
        }),
        @NamedEntityGraph(name = "Product.forCart", attributeNodes = {
                @NamedAttributeNode("primaryImage"),
                @NamedAttributeNode("taxClass"),
                @NamedAttributeNode("shippingClass")
        })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@BatchSize(size = 25)
public class Product {

    // ==================== PRIMARY KEY & VERSION ====================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    /**
     * Optimistic lock version for concurrent modification detection.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // ==================== PRODUCT TYPE & STATUS ====================

    /**
     * Type of product determining its behavior and available features.
     */
    @NotNull(message = "Product type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    @Builder.Default
    @ToString.Include
    private ProductType productType = ProductType.SIMPLE;

    /**
     * Current lifecycle status of the product.
     */
    @NotNull(message = "Product status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    @ToString.Include
    private ProductStatus status = ProductStatus.DRAFT;

    /**
     * Product condition (new, refurbished, used).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", length = 20)
    @Builder.Default
    private ProductCondition condition = ProductCondition.NEW;

    // ==================== CORE PRODUCT INFORMATION ====================

    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    @Column(name = "name", nullable = false, length = 255)
    @ToString.Include
    private String name;

    /**
     * Short description for product listings and previews.
     */
    @Size(max = 500, message = "Short description cannot exceed 500 characters")
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    /**
     * Full product description with HTML support.
     */
    @Size(max = 10000, message = "Description cannot exceed 10000 characters")
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Technical specifications or additional details.
     */
    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    // ==================== IDENTIFIERS ====================

    @Pattern(regexp = "^[A-Z0-9-]{3,50}$", message = "SKU must be 3-50 characters: uppercase letters, numbers, and hyphens")
    @Column(name = "sku", unique = true, length = 50)
    @ToString.Include
    private String sku;

    /**
     * Universal Product Code for retail.
     */
    @Pattern(regexp = "^[0-9]{12,14}$", message = "Invalid UPC/EAN format")
    @Column(name = "upc", length = 14)
    private String upc;

    /**
     * European Article Number.
     */
    @Pattern(regexp = "^[0-9]{8,13}$", message = "Invalid EAN format")
    @Column(name = "ean", length = 13)
    private String ean;

    /**
     * International Standard Book Number (for books).
     */
    @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})" +
            "[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)" +
            "(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$", message = "Invalid ISBN format")
    @Column(name = "isbn", length = 20)
    private String isbn;

    /**
     * Manufacturer Part Number.
     */
    @Size(max = 50)
    @Column(name = "mpn", length = 50)
    private String mpn;

    /**
     * Global Trade Item Number.
     */
    @Size(max = 14)
    @Column(name = "gtin", length = 14)
    private String gtin;

    // ==================== SEO & URLS ====================

    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Friendly URL must be lowercase alphanumeric with hyphens")
    @Size(max = 255)
    @Column(name = "friendly_url", unique = true, length = 255)
    private String friendlyUrl;

    @Size(max = 160, message = "Meta title should not exceed 160 characters for SEO")
    @Column(name = "meta_title", length = 160)
    private String metaTitle;

    @Size(max = 320, message = "Meta description should not exceed 320 characters for SEO")
    @Column(name = "meta_description", length = 320)
    private String metaDescription;

    @Size(max = 500)
    @Column(name = "meta_keywords", length = 500)
    private String metaKeywords;

    /**
     * Search keywords for internal search optimization.
     */
    @Column(name = "search_keywords", length = 1000)
    private String searchKeywords;

    /**
     * Canonical URL for SEO (prevents duplicate content issues).
     */
    @URL(message = "Invalid canonical URL format")
    @Column(name = "canonical_url", length = 1000)
    private String canonicalUrl;

    // ==================== PRICING ====================

    /**
     * Regular selling price.
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid price format")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    @ToString.Include
    private BigDecimal price;

    /**
     * Current discounted price (must be less than regular price).
     */
    @DecimalMin(value = "0.00", message = "Discount price cannot be negative")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "discount_price", precision = 12, scale = 2)
    private BigDecimal discountPrice;

    /**
     * Cost/purchase price for profit margin calculation.
     */
    @DecimalMin(value = "0.00", message = "Cost price cannot be negative")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    /**
     * Manufacturer's Suggested Retail Price.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "msrp", precision = 12, scale = 2)
    private BigDecimal msrp;

    /**
     * Minimum advertised price (MAP policy compliance).
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "map_price", precision = 12, scale = 2)
    private BigDecimal mapPrice;

    /**
     * Start datetime for discount price validity.
     */
    @Column(name = "discount_starts_at")
    private LocalDateTime discountStartsAt;

    /**
     * End datetime for discount price validity.
     */
    @Column(name = "discount_ends_at")
    private LocalDateTime discountEndsAt;

    /**
     * ISO 4217 currency code for this product's prices.
     */
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid ISO 4217 code")
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Whether this product is exempt from taxes.
     */
    @Column(name = "tax_exempt", nullable = false)
    @Builder.Default
    private boolean taxExempt = false;

    // ==================== INVENTORY ====================

    /**
     * Current stock quantity (for simple inventory).
     * For multi-warehouse, use ProductInventory entity.
     */
    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    /**
     * Reserved stock (items in carts/pending orders).
     */
    @Min(value = 0)
    @Column(name = "reserved_quantity")
    @Builder.Default
    private Integer reservedQuantity = 0;

    /**
     * Stock level at which to trigger reorder alert.
     */
    @Min(value = 0)
    @Column(name = "reorder_level")
    private Integer reorderLevel;

    /**
     * Quantity to reorder when stock falls below reorder level.
     */
    @Min(value = 1)
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;

    /**
     * Whether to track inventory for this product.
     */
    @Column(name = "track_inventory", nullable = false)
    @Builder.Default
    private boolean trackInventory = true;

    /**
     * Whether to allow purchases when out of stock.
     */
    @Column(name = "allow_backorder", nullable = false)
    @Builder.Default
    private boolean allowBackorder = false;

    /**
     * Maximum days for backorder delivery.
     */
    @Min(value = 1)
    @Column(name = "backorder_lead_days")
    private Integer backorderLeadDays;

    /**
     * Inventory status for display purposes.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 30)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;

    // ==================== ORDER LIMITS ====================

    /**
     * Minimum quantity per order.
     */
    @Min(value = 1)
    @Column(name = "min_order_quantity")
    @Builder.Default
    private Integer minOrderQuantity = 1;

    /**
     * Maximum quantity per order (null = unlimited).
     */
    @Min(value = 1)
    @Column(name = "max_order_quantity")
    private Integer maxOrderQuantity;

    /**
     * Quantity must be ordered in multiples of this number.
     */
    @Min(value = 1)
    @Column(name = "order_quantity_step")
    @Builder.Default
    private Integer orderQuantityStep = 1;

    // ==================== SHIPPING & DIMENSIONS ====================

    /**
     * Product weight in the specified weight unit.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 4)
    @Column(name = "weight", precision = 12, scale = 4)
    private BigDecimal weight;

    /**
     * Weight unit (KG, LB, OZ, G).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit", length = 5)
    @Builder.Default
    private WeightUnit weightUnit = WeightUnit.KG;

    /**
     * Product length in the specified dimension unit.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "length", precision = 10, scale = 2)
    private BigDecimal length;

    /**
     * Product width.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "width", precision = 10, scale = 2)
    private BigDecimal width;

    /**
     * Product height.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "height", precision = 10, scale = 2)
    private BigDecimal height;

    /**
     * Dimension unit (CM, IN, M, FT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_unit", length = 5)
    @Builder.Default
    private DimensionUnit dimensionUnit = DimensionUnit.CM;

    /**
     * Whether this product requires special shipping handling.
     */
    @Column(name = "requires_shipping", nullable = false)
    @Builder.Default
    private boolean requiresShipping = true;

    /**
     * Whether this product is fragile and needs special packaging.
     */
    @Column(name = "is_fragile", nullable = false)
    @Builder.Default
    private boolean fragile = false;

    /**
     * Whether this product contains hazardous materials.
     */
    @Column(name = "is_hazardous", nullable = false)
    @Builder.Default
    private boolean hazardous = false;

    /**
     * Shipping class for rate calculation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_class_id", foreignKey = @ForeignKey(name = "fk_product_shipping_class"))
    private ShippingClass shippingClass;

    /**
     * Country of origin for customs.
     */
    @Pattern(regexp = "^[A-Z]{2}$", message = "Country must be ISO 3166-1 alpha-2 code")
    @Column(name = "country_of_origin", length = 2)
    private String countryOfOrigin;

    /**
     * Harmonized System code for international shipping.
     */
    @Pattern(regexp = "^[0-9]{6,10}$", message = "Invalid HS code format")
    @Column(name = "hs_code", length = 10)
    private String hsCode;

    // ==================== MEDIA ====================

    /**
     * Primary/main product image.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_image_id", foreignKey = @ForeignKey(name = "fk_product_primary_image"))
    private ProductImage primaryImage;

    /**
     * Legacy single image URL (deprecated, use images collection).
     */
    @Deprecated
    @URL(message = "Invalid image URL format")
    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /**
     * URL to product video (YouTube, Vimeo, etc.).
     */
    @URL(message = "Invalid video URL format")
    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    // ==================== FLAGS & VISIBILITY ====================

    /**
     * Whether this is a master product (has variants).
     */
    @Column(name = "is_master", nullable = false)
    @Builder.Default
    private boolean isMaster = false;

    /**
     * Whether to feature this product in promotions.
     */
    @Column(name = "featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    /**
     * Whether this is a new arrival.
     */
    @Column(name = "is_new_arrival", nullable = false)
    @Builder.Default
    private boolean newArrival = false;

    /**
     * Whether this is a bestseller.
     */
    @Column(name = "is_bestseller", nullable = false)
    @Builder.Default
    private boolean bestseller = false;

    /**
     * Soft delete flag.
     */
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * When this product should become visible.
     */
    @Column(name = "visible_from")
    private LocalDateTime visibleFrom;

    /**
     * When this product should stop being visible.
     */
    @Column(name = "visible_to")
    private LocalDateTime visibleTo;

    // ==================== RATINGS & REVIEWS ====================

    /**
     * Cached average rating (0.0-5.0).
     * Updated by trigger or scheduled job from reviews.
     */
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "5.0")
    @Column(name = "average_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal averageRating = BigDecimal.ZERO;

    /**
     * Cached total number of reviews.
     */
    @Min(value = 0)
    @Column(name = "review_count")
    @Builder.Default
    private Integer reviewCount = 0;

    /**
     * Number of 5-star reviews.
     */
    @Min(value = 0)
    @Column(name = "rating_5_count")
    @Builder.Default
    private Integer rating5Count = 0;

    /**
     * Number of 4-star reviews.
     */
    @Min(value = 0)
    @Column(name = "rating_4_count")
    @Builder.Default
    private Integer rating4Count = 0;

    /**
     * Number of 3-star reviews.
     */
    @Min(value = 0)
    @Column(name = "rating_3_count")
    @Builder.Default
    private Integer rating3Count = 0;

    /**
     * Number of 2-star reviews.
     */
    @Min(value = 0)
    @Column(name = "rating_2_count")
    @Builder.Default
    private Integer rating2Count = 0;

    /**
     * Number of 1-star reviews.
     */
    @Min(value = 0)
    @Column(name = "rating_1_count")
    @Builder.Default
    private Integer rating1Count = 0;

    // ==================== ANALYTICS & POPULARITY ====================

    /**
     * Total number of views.
     */
    @Min(value = 0)
    @Column(name = "view_count")
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * Total number of purchases.
     */
    @Min(value = 0)
    @Column(name = "purchase_count")
    @Builder.Default
    private Long purchaseCount = 0L;

    /**
     * Number of times added to wishlists.
     */
    @Min(value = 0)
    @Column(name = "wishlist_count")
    @Builder.Default
    private Integer wishlistCount = 0;

    /**
     * Sort order for manual ordering.
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Popularity score for ranking (calculated).
     */
    @Column(name = "popularity_score")
    @Builder.Default
    private Double popularityScore = 0.0;

    // ==================== WARRANTY & SUPPORT ====================

    /**
     * Warranty duration in months.
     */
    @Min(value = 0)
    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    /**
     * Warranty description or terms.
     */
    @Size(max = 1000)
    @Column(name = "warranty_description", length = 1000)
    private String warrantyDescription;

    /**
     * Return policy override for this product.
     */
    @Size(max = 1000)
    @Column(name = "return_policy", length = 1000)
    private String returnPolicy;

    /**
     * Whether this product is returnable.
     */
    @Column(name = "is_returnable", nullable = false)
    @Builder.Default
    private boolean returnable = true;

    /**
     * Return window in days.
     */
    @Min(value = 0)
    @Column(name = "return_days")
    @Builder.Default
    private Integer returnDays = 30;

    // ==================== RESTRICTIONS ====================

    /**
     * Minimum age required to purchase.
     */
    @Min(value = 0)
    @Max(value = 99)
    @Column(name = "minimum_age")
    private Integer minimumAge;

    /**
     * Whether this product requires age verification.
     */
    @Column(name = "age_verification_required", nullable = false)
    @Builder.Default
    private boolean ageVerificationRequired = false;

    /**
     * Countries where this product cannot be sold (comma-separated ISO codes).
     */
    @Column(name = "restricted_countries", length = 500)
    private String restrictedCountries;

    /**
     * License or certification required to purchase.
     */
    @Column(name = "required_license", length = 100)
    private String requiredLicense;

    // ==================== DIGITAL PRODUCTS ====================

    /**
     * Whether this is a digital/downloadable product.
     */
    @Column(name = "is_digital", nullable = false)
    @Builder.Default
    private boolean digital = false;

    /**
     * URL or path to downloadable file.
     */
    @Column(name = "download_url", length = 1000)
    private String downloadUrl;

    /**
     * Maximum number of downloads allowed.
     */
    @Min(value = 0)
    @Column(name = "download_limit")
    private Integer downloadLimit;

    /**
     * Days until download link expires.
     */
    @Min(value = 0)
    @Column(name = "download_expiry_days")
    private Integer downloadExpiryDays;

    // ==================== SUBSCRIPTION PRODUCTS ====================

    /**
     * Whether this is a subscription product.
     */
    @Column(name = "is_subscription", nullable = false)
    @Builder.Default
    private boolean subscription = false;

    /**
     * Subscription billing interval.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_interval", length = 20)
    private SubscriptionInterval subscriptionInterval;

    /**
     * Number of intervals between billings.
     */
    @Min(value = 1)
    @Column(name = "subscription_interval_count")
    private Integer subscriptionIntervalCount;

    /**
     * Free trial days for subscriptions.
     */
    @Min(value = 0)
    @Column(name = "trial_days")
    private Integer trialDays;

    // ==================== CUSTOMIZATION ====================

    /**
     * Whether gift wrapping is available.
     */
    @Column(name = "gift_wrapping_available", nullable = false)
    @Builder.Default
    private boolean giftWrappingAvailable = false;

    /**
     * Additional cost for gift wrapping.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 6, fraction = 2)
    @Column(name = "gift_wrapping_price", precision = 8, scale = 2)
    private BigDecimal giftWrappingPrice;

    /**
     * Whether this product can be personalized.
     */
    @Column(name = "allow_personalization", nullable = false)
    @Builder.Default
    private boolean allowPersonalization = false;

    /**
     * Personalization options (JSON format).
     */
    @Column(name = "personalization_options", columnDefinition = "TEXT")
    private String personalizationOptions;

    // ==================== SUPPLIER INFORMATION ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", foreignKey = @ForeignKey(name = "fk_product_supplier"))
    private Supplier supplier;

    /**
     * Supplier's SKU for this product.
     */
    @Size(max = 50)
    @Column(name = "supplier_sku", length = 50)
    private String supplierSku;

    /**
     * Supplier's cost price.
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 10, fraction = 2)
    @Column(name = "supplier_cost", precision = 12, scale = 2)
    private BigDecimal supplierCost;

    /**
     * Lead time from supplier in days.
     */
    @Min(value = 0)
    @Column(name = "supplier_lead_days")
    private Integer supplierLeadDays;

    // ==================== CLASSIFICATION (LEGACY) ====================

    @Deprecated
    @Size(max = 100)
    @Column(name = "category_type", length = 100)
    private String categoryType;

    @Deprecated
    @Size(max = 100)
    @Column(name = "sub_category", length = 100)
    private String subCategory;

    // ==================== RELATIONSHIPS ====================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", foreignKey = @ForeignKey(name = "fk_product_category"))
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", foreignKey = @ForeignKey(name = "fk_product_brand"))
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", foreignKey = @ForeignKey(name = "fk_product_store"))
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id", foreignKey = @ForeignKey(name = "fk_product_tax_class"))
    private TaxClass taxClass;

    /**
     * Parent product (for variants).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_product_id", foreignKey = @ForeignKey(name = "fk_product_parent"))
    private Product parentProduct;

    /**
     * Child variants of this master product.
     */
    @OneToMany(mappedBy = "parentProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Product> variants = new LinkedHashSet<>();

    // ==================== COLLECTIONS ====================

    /**
     * Product images.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    /**
     * General product attributes as key-value pairs.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_attribute_map", joinColumns = @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_attr_map_product")), indexes = {
            @Index(name = "idx_pattr_map_product_id", columnList = "product_id"),
            @Index(name = "idx_pattr_map_name", columnList = "attribute_name"),
            @Index(name = "idx_pattr_map_value", columnList = "attribute_value")
    })
    @MapKeyColumn(name = "attribute_name", length = 100)
    @Column(name = "attribute_value", length = 2000)
    @BatchSize(size = 50)
    @Builder.Default
    private Map<String, String> attributes = new LinkedHashMap<>();

    /**
     * Variant-defining attributes (e.g., size: "XL", color: "Red").
     * These attributes differentiate variants from each other.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_variant_attributes", joinColumns = @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_vattr_product")), indexes = {
            @Index(name = "idx_pvattr_product", columnList = "product_id"),
            @Index(name = "idx_pvattr_name_value", columnList = "attribute_name, attribute_value")
    })
    @MapKeyColumn(name = "attribute_name", length = 50)
    @Column(name = "attribute_value", length = 100)
    @Builder.Default
    private Map<String, String> variantAttributes = new LinkedHashMap<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_tag_product")), inverseJoinColumns = @JoinColumn(name = "tag_id", foreignKey = @ForeignKey(name = "fk_product_tag_tag")), indexes = {
            @Index(name = "idx_product_tags_product", columnList = "product_id"),
            @Index(name = "idx_product_tags_tag", columnList = "tag_id")
    })
    @BatchSize(size = 50)
    @Builder.Default
    private Set<Tag> tags = new LinkedHashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductReview> reviews = new ArrayList<>();

    /**
     * Multi-warehouse inventory records.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private Set<ProductInventory> inventoryRecords = new HashSet<>();

    /**
     * Related products (accessories, alternatives).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_related", joinColumns = @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_related_product")), inverseJoinColumns = @JoinColumn(name = "related_product_id", foreignKey = @ForeignKey(name = "fk_related_related")))
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Product> relatedProducts = new LinkedHashSet<>();

    /**
     * Cross-sell products (frequently bought together).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_cross_sells", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "cross_sell_product_id"))
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Product> crossSellProducts = new LinkedHashSet<>();

    /**
     * Up-sell products (premium alternatives).
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_up_sells", joinColumns = @JoinColumn(name = "product_id"), inverseJoinColumns = @JoinColumn(name = "up_sell_product_id"))
    @BatchSize(size = 20)
    @Builder.Default
    private Set<Product> upSellProducts = new LinkedHashSet<>();

    /**
     * Price history for analytics and undo.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt DESC")
    @BatchSize(size = 20)
    @Builder.Default
    private List<ProductPriceHistory> priceHistory = new ArrayList<>();

    // ==================== AUDIT FIELDS ====================

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    // ==================== LIFECYCLE CALLBACKS ====================

    @PrePersist
    protected void onCreate() {
        if (stockQuantity == null) {
            stockQuantity = 0;
        }
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (sku == null || sku.isBlank()) {
            sku = generateSku();
        }
        if (friendlyUrl == null || friendlyUrl.isBlank()) {
            friendlyUrl = generateFriendlyUrl();
        }
        validatePrices();
        updateStockStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        validatePrices();
        updateStockStatus();
    }

    private void validatePrices() {
        if (discountPrice != null && price != null && discountPrice.compareTo(price) >= 0) {
            throw new IllegalStateException("Discount price must be less than regular price");
        }
        if (costPrice != null && price != null && costPrice.compareTo(price) > 0) {
            // Warning: selling below cost - could log this
        }
    }

    private String generateSku() {
        String prefix = "PRD";
        if (category != null) {
            if (category.getSlug() != null && !category.getSlug().isBlank()) {
                prefix = category.getSlug().toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0,
                        Math.min(3, category.getSlug().length()));
            } else if (category.getName() != null && !category.getName().isBlank()) {
                prefix = category.getName().toUpperCase().replaceAll("[^A-Z0-9]", "").substring(0,
                        Math.min(3, category.getName().length()));
            }
        }
        return String.format("%s-%d-%s", prefix, System.currentTimeMillis() % 100000,
                UUID.randomUUID().toString().substring(0, 4).toUpperCase());
    }

    private String generateFriendlyUrl() {
        if (name == null)
            return null;
        return name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // ==================== BUSINESS METHODS ====================

    /**
     * Gets the effective selling price considering active discounts.
     * 
     * @return the current selling price
     */
    public BigDecimal getEffectivePrice() {
        if (!isDiscountActive()) {
            return price;
        }
        return discountPrice;
    }

    /**
     * Checks if discount is currently active based on time window.
     */
    public boolean isDiscountActive() {
        if (discountPrice == null || price == null || discountPrice.compareTo(price) >= 0) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        if (discountStartsAt != null && now.isBefore(discountStartsAt)) {
            return false;
        }
        if (discountEndsAt != null && now.isAfter(discountEndsAt)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the product is currently in stock.
     */
    public boolean isInStock() {
        if (!trackInventory) {
            return true;
        }
        return getAvailableQuantity() > 0;
    }

    /**
     * Gets the quantity available for purchase (total - reserved).
     */
    public int getAvailableQuantity() {
        int stock = stockQuantity != null ? stockQuantity : 0;
        int reserved = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, stock - reserved);
    }

    /**
     * Checks if the product has an active discount.
     */
    public boolean hasDiscount() {
        return isDiscountActive();
    }

    /**
     * Calculates the discount percentage.
     */
    public BigDecimal getDiscountPercentage() {
        if (!hasDiscount()) {
            return BigDecimal.ZERO;
        }
        return price.subtract(discountPrice)
                .divide(price, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets the discount amount in currency.
     */
    public BigDecimal getDiscountAmount() {
        if (!hasDiscount()) {
            return BigDecimal.ZERO;
        }
        return price.subtract(discountPrice);
    }

    /**
     * Calculates profit margin percentage based on cost price.
     */
    public BigDecimal getProfitMarginPercentage() {
        if (costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        BigDecimal effectivePrice = getEffectivePrice();
        return effectivePrice.subtract(costPrice)
                .divide(effectivePrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Gets the profit amount per unit.
     */
    public BigDecimal getProfitAmount() {
        if (costPrice == null) {
            return null;
        }
        return getEffectivePrice().subtract(costPrice);
    }

    /**
     * Checks if the product can be purchased.
     */
    public boolean isPurchasable() {
        if (status != ProductStatus.ACTIVE || deleted) {
            return false;
        }
        if (!isCurrentlyVisible()) {
            return false;
        }
        if (trackInventory && !isInStock() && !allowBackorder) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the product is currently visible based on time window.
     */
    public boolean isCurrentlyVisible() {
        LocalDateTime now = LocalDateTime.now();
        if (visibleFrom != null && now.isBefore(visibleFrom)) {
            return false;
        }
        if (visibleTo != null && now.isAfter(visibleTo)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the product is available for display.
     */
    public boolean isAvailable() {
        return status == ProductStatus.ACTIVE && !deleted && isCurrentlyVisible();
    }

    /**
     * Checks if this product requires age verification for purchase.
     */
    public boolean requiresAgeVerification() {
        return ageVerificationRequired || (minimumAge != null && minimumAge > 0);
    }

    /**
     * Checks if product can be shipped to the specified country.
     */
    public boolean canShipTo(String countryCode) {
        if (restrictedCountries == null || restrictedCountries.isBlank()) {
            return true;
        }
        return !Arrays.asList(restrictedCountries.split(","))
                .contains(countryCode.toUpperCase());
    }

    /**
     * Validates the order quantity against product constraints.
     */
    public void validateOrderQuantity(int quantity) {
        if (quantity < minOrderQuantity) {
            throw new IllegalArgumentException(
                    String.format("Minimum order quantity is %d", minOrderQuantity));
        }
        if (maxOrderQuantity != null && quantity > maxOrderQuantity) {
            throw new IllegalArgumentException(
                    String.format("Maximum order quantity is %d", maxOrderQuantity));
        }
        if (orderQuantityStep != null && orderQuantityStep > 1) {
            if ((quantity - minOrderQuantity) % orderQuantityStep != 0) {
                throw new IllegalArgumentException(
                        String.format("Quantity must be ordered in steps of %d", orderQuantityStep));
            }
        }
        if (trackInventory && !allowBackorder && quantity > getAvailableQuantity()) {
            throw new IllegalArgumentException(
                    String.format("Only %d items available", getAvailableQuantity()));
        }
    }

    // ==================== STOCK MANAGEMENT ====================

    /**
     * Reserves stock for a pending order.
     * Thread-safe when used with @Version optimistic locking.
     *
     * @param quantity amount to reserve
     * @return true if reservation successful
     */
    public boolean reserveStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!trackInventory) {
            return true;
        }
        int available = getAvailableQuantity();
        if (available < quantity && !allowBackorder) {
            return false;
        }
        this.reservedQuantity = (reservedQuantity != null ? reservedQuantity : 0) + quantity;
        updateStockStatus();
        return true;
    }

    /**
     * Releases reserved stock (e.g., order cancelled).
     */
    public void releaseReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.reservedQuantity = Math.max(0,
                (reservedQuantity != null ? reservedQuantity : 0) - quantity);
        updateStockStatus();
    }

    /**
     * Commits reserved stock (converts reservation to actual sale).
     * Decreases both stock and reserved quantities.
     */
    public void commitReservedStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (trackInventory) {
            this.stockQuantity = Math.max(0,
                    (stockQuantity != null ? stockQuantity : 0) - quantity);
            this.reservedQuantity = Math.max(0,
                    (reservedQuantity != null ? reservedQuantity : 0) - quantity);
        }
        this.purchaseCount = (purchaseCount != null ? purchaseCount : 0L) + quantity;
        updateStockStatus();
    }

    /**
     * Decreases stock by the specified quantity.
     * Thread-safe when used with @Version optimistic locking.
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!trackInventory) {
            return;
        }
        int available = getAvailableQuantity();
        if (available < quantity && !allowBackorder) {
            throw new InsufficientStockException(
                    String.format("Insufficient stock. Available: %d, Requested: %d", available, quantity),
                    this.id, available, quantity);
        }
        this.stockQuantity = Math.max(0, (stockQuantity != null ? stockQuantity : 0) - quantity);
        updateStockStatus();
    }

    /**
     * Increases stock by the specified quantity.
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (!trackInventory) {
            return;
        }
        this.stockQuantity = (stockQuantity != null ? stockQuantity : 0) + quantity;
        updateStockStatus();
    }

    /**
     * Updates stock status based on current inventory levels.
     */
    public void updateStockStatus() {
        if (!trackInventory) {
            this.stockStatus = StockStatus.IN_STOCK;
            return;
        }

        int available = getAvailableQuantity();

        if (available <= 0) {
            this.stockStatus = allowBackorder ? StockStatus.ON_BACKORDER : StockStatus.OUT_OF_STOCK;
        } else if (reorderLevel != null && available <= reorderLevel) {
            this.stockStatus = StockStatus.LOW_STOCK;
        } else {
            this.stockStatus = StockStatus.IN_STOCK;
        }
    }

    /**
     * Checks if stock is below reorder level.
     */
    public boolean needsReorder() {
        if (!trackInventory || reorderLevel == null) {
            return false;
        }
        return getAvailableQuantity() <= reorderLevel;
    }

    // ==================== RATING MANAGEMENT ====================

    /**
     * Recalculates rating statistics from reviews.
     * Should be called after review add/update/delete.
     */
    public void recalculateRating() {
        if (reviews == null || reviews.isEmpty()) {
            this.averageRating = BigDecimal.ZERO;
            this.reviewCount = 0;
            this.rating1Count = 0;
            this.rating2Count = 0;
            this.rating3Count = 0;
            this.rating4Count = 0;
            this.rating5Count = 0;
            return;
        }

        int[] counts = new int[5];
        int total = 0;
        int sum = 0;

        for (ProductReview review : reviews) {
            if (review.isApproved() && review.getRating() != null) {
                int rating = review.getRating();
                if (rating >= 1 && rating <= 5) {
                    counts[rating - 1]++;
                    sum += rating;
                    total++;
                }
            }
        }

        this.rating1Count = counts[0];
        this.rating2Count = counts[1];
        this.rating3Count = counts[2];
        this.rating4Count = counts[3];
        this.rating5Count = counts[4];
        this.reviewCount = total;

        this.averageRating = total > 0
                ? BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    // ==================== RELATIONSHIP MANAGEMENT ====================

    /**
     * Adds a review to this product (maintains bidirectional relationship).
     */
    public void addReview(ProductReview review) {
        Objects.requireNonNull(review, "Review cannot be null");
        reviews.add(review);
        review.setProduct(this);
        recalculateRating();
    }

    /**
     * Removes a review from this product.
     */
    public void removeReview(ProductReview review) {
        if (review != null && reviews.remove(review)) {
            review.setProduct(null);
            recalculateRating();
        }
    }

    /**
     * Adds a tag to this product (maintains bidirectional relationship).
     */
    public void addTag(Tag tag) {
        Objects.requireNonNull(tag, "Tag cannot be null");
        tags.add(tag);
        if (tag.getProducts() != null) {
            tag.getProducts().add(this);
        }
    }

    /**
     * Removes a tag from this product.
     */
    public void removeTag(Tag tag) {
        if (tag != null && tags.remove(tag)) {
            if (tag.getProducts() != null) {
                tag.getProducts().remove(this);
            }
        }
    }

    /**
     * Clears all tags.
     */
    public void clearTags() {
        for (Tag tag : new HashSet<>(tags)) {
            removeTag(tag);
        }
    }

    /**
     * Adds a variant to this master product.
     */
    public void addVariant(Product variant) {
        Objects.requireNonNull(variant, "Variant cannot be null");
        if (!this.isMaster) {
            throw new IllegalStateException("Only master products can have variants");
        }
        variants.add(variant);
        variant.setParentProduct(this);
    }

    /**
     * Removes a variant from this master product.
     */
    public void removeVariant(Product variant) {
        if (variant != null && variants.remove(variant)) {
            variant.setParentProduct(null);
        }
    }

    /**
     * Adds an image to this product.
     */
    public void addImage(ProductImage image) {
        Objects.requireNonNull(image, "Image cannot be null");
        images.add(image);
        image.setProduct(this);
        if (images.size() == 1) {
            this.primaryImage = image;
        }
    }

    /**
     * Removes an image from this product.
     */
    public void removeImage(ProductImage image) {
        if (image != null && images.remove(image)) {
            image.setProduct(null);
            if (Objects.equals(primaryImage, image)) {
                this.primaryImage = images.isEmpty() ? null : images.get(0);
            }
        }
    }

    /**
     * Adds a related product.
     */
    public void addRelatedProduct(Product related) {
        Objects.requireNonNull(related, "Related product cannot be null");
        if (!Objects.equals(this.id, related.getId())) {
            relatedProducts.add(related);
        }
    }

    /**
     * Adds a cross-sell product.
     */
    public void addCrossSellProduct(Product crossSell) {
        Objects.requireNonNull(crossSell, "Cross-sell product cannot be null");
        if (!Objects.equals(this.id, crossSell.getId())) {
            crossSellProducts.add(crossSell);
        }
    }

    /**
     * Adds an up-sell product.
     */
    public void addUpSellProduct(Product upSell) {
        Objects.requireNonNull(upSell, "Up-sell product cannot be null");
        if (!Objects.equals(this.id, upSell.getId())) {
            upSellProducts.add(upSell);
        }
    }

    // ==================== SOFT DELETE OPERATIONS ====================

    /**
     * Marks this product as deleted (soft delete).
     */
    public void markAsDeleted(String deletedByUser) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedByUser;
        this.status = ProductStatus.DISCONTINUED;
    }

    /**
     * Marks this product as deleted (soft delete).
     */
    public void markAsDeleted() {
        markAsDeleted(null);
    }

    /**
     * Restores a soft-deleted product.
     */
    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.status = ProductStatus.DRAFT;
    }

    /**
     * Permanently deactivates the product without deleting.
     */
    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    /**
     * Activates the product.
     */
    public void activate() {
        if (!deleted) {
            this.status = ProductStatus.ACTIVE;
        }
    }

    /**
     * Sets product to draft status.
     */
    public void setToDraft() {
        if (!deleted) {
            this.status = ProductStatus.DRAFT;
        }
    }

    // ==================== ATTRIBUTE MANAGEMENT ====================

    /**
     * Gets an attribute value by name.
     */
    public Optional<String> getAttribute(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * Sets an attribute value.
     */
    public void setAttribute(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attribute name cannot be null or blank");
        }
        if (value == null) {
            attributes.remove(name);
        } else {
            attributes.put(name.trim(), value.trim());
        }
    }

    /**
     * Sets multiple attributes at once.
     */
    public void setAttributes(Map<String, String> newAttributes) {
        if (newAttributes != null) {
            newAttributes.forEach(this::setAttribute);
        }
    }

    /**
     * Removes an attribute.
     */
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    /**
     * Checks if an attribute exists.
     */
    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    /**
     * Gets all attribute names.
     */
    public Set<String> getAttributeNames() {
        return Collections.unmodifiableSet(attributes.keySet());
    }

    // ==================== VARIANT ATTRIBUTE MANAGEMENT ====================

    /**
     * Sets a variant attribute (e.g., size, color).
     */
    public void setVariantAttribute(String name, String value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Variant attribute name cannot be null or blank");
        }
        if (value == null) {
            variantAttributes.remove(name);
        } else {
            variantAttributes.put(name.trim(), value.trim());
        }
    }

    /**
     * Gets variant attribute value.
     */
    public Optional<String> getVariantAttribute(String name) {
        return Optional.ofNullable(variantAttributes.get(name));
    }

    // ==================== SHIPPING CALCULATIONS ====================

    /**
     * Calculates volumetric weight for shipping.
     */
    public BigDecimal getVolumetricWeight() {
        if (length == null || width == null || height == null) {
            return null;
        }
        // DIM factor: 5000 for metric (cm/kg), 139 for imperial (in/lb)
        BigDecimal dimFactor = dimensionUnit == DimensionUnit.CM
                ? BigDecimal.valueOf(5000)
                : BigDecimal.valueOf(139);

        return length.multiply(width).multiply(height)
                .divide(dimFactor, 2, RoundingMode.CEILING);
    }

    /**
     * Gets the billable weight (greater of actual or volumetric).
     */
    public BigDecimal getBillableWeight() {
        BigDecimal volumetricWeight = getVolumetricWeight();
        if (weight == null) {
            return volumetricWeight;
        }
        if (volumetricWeight == null) {
            return weight;
        }
        return weight.max(volumetricWeight);
    }

    // ==================== ANALYTICS ====================

    /**
     * Increments view count (thread-safe with version).
     */
    public void incrementViewCount() {
        this.viewCount = (viewCount != null ? viewCount : 0L) + 1;
    }

    /**
     * Increments wishlist count.
     */
    public void incrementWishlistCount() {
        this.wishlistCount = (wishlistCount != null ? wishlistCount : 0) + 1;
    }

    /**
     * Decrements wishlist count.
     */
    public void decrementWishlistCount() {
        this.wishlistCount = Math.max(0, (wishlistCount != null ? wishlistCount : 0) - 1);
    }

    /**
     * Recalculates popularity score based on various factors and weights.
     */
    public void recalculatePopularityScore(double purchaseWeight, double viewWeight, double ratingWeight,
            double wishlistWeight) {
        double score = 0.0;

        score += (purchaseCount != null ? purchaseCount : 0) * purchaseWeight;
        score += (viewCount != null ? viewCount : 0) * viewWeight;
        score += (averageRating != null ? averageRating.doubleValue() : 0) * 5 * (reviewCount != null ? reviewCount : 0)
                * ratingWeight;
        score += (wishlistCount != null ? wishlistCount : 0) * wishlistWeight;

        this.popularityScore = score;
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Creates a deep copy of this product for variant creation.
     */
    public Product createVariantCopy() {
        return Product.builder()
                .name(this.name)
                .shortDescription(this.shortDescription)
                .description(this.description)
                .price(this.price)
                .costPrice(this.costPrice)
                .category(this.category)
                .brand(this.brand)
                .store(this.store)
                .taxClass(this.taxClass)
                .productType(ProductType.VARIANT)
                .status(ProductStatus.DRAFT)
                .parentProduct(this)
                .isMaster(false)
                .weight(this.weight)
                .weightUnit(this.weightUnit)
                .requiresShipping(this.requiresShipping)
                .trackInventory(this.trackInventory)
                .attributes(new LinkedHashMap<>(this.attributes))
                .build();
    }

    /**
     * Checks if this is a variant product.
     */
    public boolean isVariant() {
        return parentProduct != null;
    }

    /**
     * Gets the master product (self if master, parent if variant).
     */
    public Product getMasterProduct() {
        return isVariant() ? parentProduct : this;
    }

    // ==================== BACKWARD COMPATIBILITY METHODS ====================

    /**
     * @deprecated Use status field instead. This method checks if status is ACTIVE.
     */
    @Deprecated
    public boolean isActive() {
        return status == ProductStatus.ACTIVE && !deleted;
    }

    /**
     * @deprecated Use setStatus(ProductStatus.ACTIVE) or activate() method instead.
     */
    @Deprecated
    public void setActive(Boolean active) {
        if (Boolean.TRUE.equals(active)) {
            activate();
        } else {
            deactivate();
        }
    }

    /**
     * @deprecated Use attributes field instead. Returns a copy of general
     *             attributes.
     */
    @Deprecated
    public Map<String, String> getCategoryAttributes() {
        return new LinkedHashMap<>(attributes);
    }

    /**
     * @deprecated Use attributes field instead. Sets general attributes.
     */
    @Deprecated
    public void setCategoryAttributes(Map<String, String> categoryAttributes) {
        if (categoryAttributes != null) {
            this.attributes.putAll(categoryAttributes);
        }
    }
}
