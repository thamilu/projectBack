package com.eshop.app.repository.projection;

import java.math.BigDecimal;

/**
 * Lightweight projection for product list views.
 * 
 * <p>Uses Spring Data JPA interface-based projection for optimal performance.
 * Only selects required fields from the database, avoiding unnecessary joins and data fetching.
 * 
 * <p>Use this projection for:
 * <ul>
 *   <li>Product listing pages</li>
 *   <li>Search results</li>
 *   <li>Category browse pages</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0
 */
public interface ProductSummaryProjection {
    
    Long getId();
    
    String getName();
    
    String getSku();
    
    String getFriendlyUrl();
    
    BigDecimal getPrice();
    
    BigDecimal getDiscountPrice();
    
    String getImageUrl();
    
    Boolean getActive();
    
    Boolean getFeatured();
    
    Integer getStockQuantity();
    
    /**
     * Category name via nested projection.
     */
    CategoryInfo getCategory();
    
    /**
     * Brand name via nested projection.
     */
    BrandInfo getBrand();
    
    /**
     * Nested projection for category.
     */
    interface CategoryInfo {
        String getName();
    }
    
    /**
     * Nested projection for brand.
     */
    interface BrandInfo {
        String getName();
    }
}
