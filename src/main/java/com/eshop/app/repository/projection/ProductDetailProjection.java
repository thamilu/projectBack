package com.eshop.app.repository.projection;

import com.eshop.app.entity.enums.ProductStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO-based projection for product details using constructor expression.
 * 
 * <p>
 * This is a class-based projection that can be used in JPQL queries
 * with constructor expression for optimal performance.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * &#64;Query("""
 *             SELECT new com.eshop.app.repository.projection.ProductDetailProjection(
 *                 p.id, p.name, p.description, p.sku, p.friendlyUrl,
 *                 p.price, p.discountPrice, p.stockQuantity, p.imageUrl,
 *                 p.status, p.featured, p.isMaster,
 *                 c.id, c.name, b.id, b.name, s.id, s.storeName,
 *                 p.createdAt, p.updatedAt, p.version
 *             )
 *             FROM Product p
 *             LEFT JOIN p.category c
 *             LEFT JOIN p.brand b
 *             LEFT JOIN p.shop s
 *             WHERE p.id = :id AND p.deleted = false
 *         """)
 * Optional&lt;ProductDetailProjection&gt; findDetailById(Long id);
 * </pre>
 * 
 * @author E-Shop Team
 * @version 2.0
 */
public class ProductDetailProjection {
    
    private final Long id;
    private final String name;
    private final String description;
    private final String sku;
    private final String friendlyUrl;
    private final BigDecimal price;
    private final BigDecimal discountPrice;
    private final Integer stockQuantity;
    private final String imageUrl;
    private final ProductStatus status;
    private final Boolean featured;
    private final Boolean isMaster;
    private final Long categoryId;
    private final String categoryName;
    private final Long brandId;
    private final String brandName;
    private final Long storeId;
    private final String storeName;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Long version;
    
    public ProductDetailProjection(
            Long id, String name, String description, String sku, String friendlyUrl,
            BigDecimal price, BigDecimal discountPrice, Integer stockQuantity, String imageUrl,
            ProductStatus status, Boolean featured, Boolean isMaster,
            Long categoryId, String categoryName, Long brandId, String brandName,
            Long storeId, String storeName,
            LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sku = sku;
        this.friendlyUrl = friendlyUrl;
        this.price = price;
        this.discountPrice = discountPrice;
        this.stockQuantity = stockQuantity;
        this.imageUrl = imageUrl;
        this.status = status;
        this.featured = featured;
        this.isMaster = isMaster;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.brandId = brandId;
        this.brandName = brandName;
        this.storeId = storeId;
        this.storeName = storeName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getSku() { return sku; }
    public String getFriendlyUrl() { return friendlyUrl; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getDiscountPrice() { return discountPrice; }
    public Integer getStockQuantity() { return stockQuantity; }
    public String getImageUrl() { return imageUrl; }
    public ProductStatus getStatus() { return status; }
    public Boolean getFeatured() { return featured; }
    public Boolean getIsMaster() { return isMaster; }
    public Long getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Long getBrandId() { return brandId; }
    public String getBrandName() { return brandName; }
    public Long getStoreId() { return storeId; }
    public String getStoreName() { return storeName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
