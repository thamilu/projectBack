package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.response.ProductStatistics;
import com.eshop.app.catalog.api.response.TopSellingProductResponse;
import com.eshop.app.catalog.application.dto.ProductStatisticsDTO;
import com.eshop.app.catalog.application.mapper.ProductMapper;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductStatus;
import com.eshop.app.catalog.domain.repository.BrandRepository;
import com.eshop.app.catalog.domain.repository.CategoryRepository;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.domain.repository.ProductRepositoryEnhanced;
import com.eshop.app.catalog.domain.repository.TagRepository;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import com.eshop.app.seller.api.response.SellerProductDashboard;
import com.eshop.app.seller.api.response.TopProductResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class ProductAnalyticsService {

    private final ProductRepository productRepository;
    private final ProductRepositoryEnhanced productRepositoryEnhanced;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final TagRepository tagRepository;
    private final ProductMapper productMapper;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN)
    public ProductStatistics getGlobalStatistics() {
        long totalProducts = productRepository.count();
        long activeProducts = productRepository.countByStatus(ProductStatus.ACTIVE);
        long inactiveProducts = productRepository.countByStatus(ProductStatus.INACTIVE);
        long featuredProducts = productRepository.countByFeatured(true);
        long outOfStock = productRepository.countByStockQuantity(0);
        long lowStock =
                productRepository.countByStockQuantityLessThan(LOW_STOCK_THRESHOLD);
        long totalInventoryUnits = productRepository.sumStockQuantity();
        double avgStock = totalProducts > 0 ? (double) totalInventoryUnits / totalProducts : 0.0;
        BigDecimal avgPrice = productRepository.avgPrice();
        BigDecimal minPrice = productRepository.minPrice();
        BigDecimal maxPrice = productRepository.maxPrice();
        BigDecimal totalInventoryValue = productRepository.sumInventoryValue();
        long totalCategories = categoryRepository.count();
        long totalBrands = brandRepository.count();
        long totalTags = tagRepository.count();
        LocalDateTime now = LocalDateTime.now();
        long productsAdded24h = productRepository.countByCreatedAtAfter(now.minusHours(24));
        long productsUpdated24h = productRepository.countByUpdatedAtAfter(now.minusHours(24));

        return ProductStatistics.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(inactiveProducts)
                .featuredProducts(featuredProducts)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .totalInventoryUnits(totalInventoryUnits)
                .averageStockPerProduct(avgStock)
                .averagePrice(avgPrice)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .totalInventoryValue(totalInventoryValue)
                .totalCategories(totalCategories)
                .totalBrands(totalBrands)
                .totalTags(totalTags)
                .generatedAt(now)
                .productsAddedLast24Hours(productsAdded24h)
                .productsUpdatedLast24Hours(productsUpdated24h)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN_OR_SELLER_SELF_BY_SELLER_ID)
    public SellerProductDashboard getSellerDashboard(Long sellerId, int topProductsLimit) {
        long totalProducts = productRepository.countByStoreSellerProfileUserId(sellerId);
        long activeProducts =
                productRepository.countByStoreSellerProfileUserIdAndStatus(
                        sellerId, ProductStatus.ACTIVE);
        long outOfStock =
                productRepository.countByStoreSellerProfileUserIdAndStockQuantity(
                        sellerId, 0);
        long lowStock =
                productRepository
                        .countByStoreSellerProfileUserIdAndStockQuantityLessThan(
                                sellerId, LOW_STOCK_THRESHOLD);
        long featuredProducts =
                productRepository.countByStoreSellerProfileUserIdAndFeaturedTrue(sellerId);
        long totalInventoryUnits = productRepository.sumStockQuantityBySellerId(sellerId);
        double avgStock = totalProducts > 0 ? (double) totalInventoryUnits / totalProducts : 0.0;
        BigDecimal totalInventoryValue = productRepository.sumInventoryValueBySellerId(sellerId);
        BigDecimal avgPrice = productRepository.avgPriceBySellerId(sellerId);
        BigDecimal highestPrice = productRepository.maxPriceBySellerId(sellerId);
        BigDecimal lowestPrice = productRepository.minPriceBySellerId(sellerId);
        List<TopSellingProductResponse> topSelling = getTopSellingProducts(topProductsLimit);
        List<SellerProductDashboard.TopRatedProduct> topRated = Collections.emptyList();
        LocalDateTime now = LocalDateTime.now();
        long productsAdded30d =
                productRepository.countByStoreSellerProfileUserIdAndCreatedAtAfter(
                        sellerId, now.minusDays(30));
        long productsUpdated30d =
                productRepository.countByStoreSellerProfileUserIdAndUpdatedAtAfter(
                        sellerId, now.minusDays(30));

        return SellerProductDashboard.builder()
                .sellerId(sellerId)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .inactiveProducts(totalProducts - activeProducts)
                .featuredProducts(featuredProducts)
                .outOfStockCount(outOfStock)
                .lowStockCount(lowStock)
                .totalInventoryUnits(totalInventoryUnits)
                .averageStockPerProduct(avgStock)
                .totalInventoryValue(totalInventoryValue)
                .averagePrice(avgPrice)
                .highestPrice(highestPrice)
                .lowestPrice(lowestPrice)
                .topSellingProducts(topSelling)
                .topRatedProducts(topRated)
                .productsAddedLast30Days(productsAdded30d)
                .productsUpdatedLast30Days(productsUpdated30d)
                .generatedAt(now)
                .build();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public Optional<String> getFavoriteCategoryByCustomerId(Long customerId) {
        List<Object[]> result = productRepository.findFavoriteCategoryByCustomerId(customerId);
        if (result.isEmpty()) return Optional.empty();
        return Optional.ofNullable((String) result.getFirst()[0]);
    }

    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN_OR_SELLER_OR_CUSTOMER)
    public List<TopSellingProductResponse> getTopSellingProducts(int limit) {
        List<Product> products = productRepository.findTopSellingProducts(PageRequest.of(0, limit));
        List<TopSellingProductResponse> result = new ArrayList<>();
        int rank = 1;
        for (Product p : products) {
            result.add(
                    TopSellingProductResponse.builder()
                            .productId(p.getId())
                            .productName(p.getName())
                            .sku(p.getSku())
                            .friendlyUrl(p.getFriendlyUrl())
                            .categoryName(
                                    p.getCategory() != null ? p.getCategory().getName() : null)
                            .brandName(p.getBrand() != null ? p.getBrand().getName() : null)
                            .currentPrice(p.getPrice())
                            .discountPrice(p.getDiscountPrice())
                            .imageUrl(productMapper.getPrimaryImageUrl(p))
                            .totalQuantitySold(0L)
                            .totalRevenue(BigDecimal.ZERO)
                            .averageOrderQuantity(0.0)
                            .uniqueOrderCount(0L)
                            .rank(rank++)
                            .salesPercentage(0.0)
                            .averageRating(0.0)
                            .reviewCount(0L)
                            .currentStock(p.getStockQuantity())
                            .stockStatus(
                                    p.getStockQuantity() == 0
                                            ? "OUT_OF_STOCK"
                                            : (p.getStockQuantity() < LOW_STOCK_THRESHOLD
                                                    ? "LOW_STOCK"
                                                    : "IN_STOCK"))
                            .sellerId(
                                    p.getStore() != null
                                                    && p.getStore().getSellerProfile() != null
                                                    && p.getStore().getSellerProfile().getUser()
                                                            != null
                                            ? p.getStore().getSellerProfile().getUser().getId()
                                            : null)
                            .storeName(p.getStore() != null ? p.getStore().getStoreName() : null)
                            .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long getTotalProductCount() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public long getProductCountBySellerId(Long sellerId) {
        return productRepository.countByStoreSellerProfileUserId(sellerId);
    }

    @Transactional(readOnly = true)
    public long getActiveProductCountBySellerId(Long sellerId) {
        return productRepository.countByStoreSellerProfileUserIdAndStatus(
                sellerId, ProductStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public long getOutOfStockCountBySellerId(Long sellerId) {
        return productRepository.countByStoreSellerProfileUserIdAndStockQuantity(
                sellerId, 0);
    }

    @Transactional(readOnly = true)
    public long getLowStockCountBySellerId(Long sellerId) {
        return productRepository
                .countByStoreSellerProfileUserIdAndStockQuantityLessThan(
                        sellerId, LOW_STOCK_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductPerformanceBySellerId(Long sellerId) {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Product> products = productRepository.findByStoreSellerProfileUserId(sellerId);
        for (Product p : products) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", p.getId());
            m.put("productName", p.getName());
            m.put("sku", p.getSku());
            m.put("currentStock", p.getStockQuantity());
            m.put("totalSold", 0L);
            result.add(m);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ProductStatisticsDTO getProductStatisticsBySellerId(Long sellerId) {
        Map<String, Object> map =
                productRepositoryEnhanced.getProductStatisticsBySellerId(sellerId);
        return ProductStatisticsDTO.builder()
                .totalProducts(getLongValue(map, "totalProducts"))
                .activeProducts(getLongValue(map, "activeProducts"))
                .averageRating(getDoubleValue(map, "averageRating"))
                .build();
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> getTopProductsBySellerId(Long sellerId, int limit) {
        List<Map<String, Object>> rawProducts =
                productRepositoryEnhanced.getTopSellingProductsBySellerId(
                        sellerId, PageRequest.of(0, limit));
        return rawProducts.stream().map(this::mapToTopProductResponse).toList();
    }

    private TopProductResponse mapToTopProductResponse(Map<String, Object> map) {
        if (map == null) return null;

        Long productId = null;
        Object pId = map.get("productId");
        if (pId instanceof Number) {
            productId = ((Number) pId).longValue();
        }

        String productName = getStringValue(map, "productName");
        String sku = getStringValue(map, "sku");

        Long unitsSold = 0L;
        Object uSold = map.get("totalSold");
        if (uSold instanceof Number) {
            unitsSold = ((Number) uSold).longValue();
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        Object rev = map.get("revenue");
        if (rev instanceof BigDecimal) {
            totalRevenue = (BigDecimal) rev;
        } else if (rev instanceof Number) {
            totalRevenue = BigDecimal.valueOf(((Number) rev).doubleValue());
        }

        return TopProductResponse.builder()
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .unitsSold(unitsSold)
                .totalRevenue(totalRevenue)
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        if (map == null) return 0L;
        Object value = map.get(key);
        if (value == null) return 0L;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        if (map == null) return 0.0;
        Object value = map.get(key);
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }
}
