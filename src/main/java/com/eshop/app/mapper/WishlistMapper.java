package com.eshop.app.mapper;

import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.WishlistResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.ProductImage;
import com.eshop.app.entity.Wishlist;
import com.eshop.app.entity.enums.ProductStatus;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Dedicated mapper for {@link Wishlist} → {@link WishlistResponse}.
 * <p>
 * Extracted from the private helpers in {@code WishlistServiceImpl} to follow
 * the single-responsibility principle and DRY principle.
 * The {@link #toPageResponse(Page)} method uses the shared {@link PageResponse#of} factory
 * instead of building {@link PageResponse.PageMetadata} inline.
 */
@Component
public class WishlistMapper {

    /**
     * Convert a {@link Wishlist} entity to its API response DTO.
     */
    public WishlistResponse toResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();

        WishlistResponse.ProductDetails productDetails = null;
        if (product != null) {
            boolean active = isProductActive(product);
            boolean inStock = product.getStockQuantity() != null && product.getStockQuantity() > 0;

            productDetails = WishlistResponse.ProductDetails.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .description(product.getDescription())
                    .price(product.getPrice())
                    .discountPrice(product.getDiscountPrice())
                    .imageUrl(getPrimaryImageUrl(product))
                    .isActive(active)
                    .stockQuantity(product.getStockQuantity())
                    .inStock(inStock)
                    .storeName(product.getStore() != null ? product.getStore().getStoreName() : null)
                    .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                    .isAvailable(active && inStock)
                    .availabilityMessage(active
                            ? (inStock ? "In Stock" : "Out of Stock")
                            : "Product Not Available")
                    .build();
        }

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .userId(wishlist.getUser().getId())
                .productId(wishlist.getProduct().getId())
                .notes(wishlist.getNotes())
                .createdAt(wishlist.getCreatedAt())
                .product(productDetails)
                .build();
    }

    /**
     * Convert a {@link Page} of {@link Wishlist} entities to a {@link PageResponse}.
     * Delegates to the shared {@link PageResponse#of(Page, java.util.function.Function)} factory
     * to avoid duplicating pagination metadata construction.
     */
    public PageResponse<WishlistResponse> toPageResponse(Page<Wishlist> page) {
        return PageResponse.of(page, this::toResponse);
    }

    // ─── private helpers ─────────────────────────────────────────────────────

    private String getPrimaryImageUrl(Product product) {
        if (product == null) return null;
        if (product.getPrimaryImage() != null) return product.getPrimaryImage().getUrl();
        try {
            if (product.getImages() != null
                    && Hibernate.isInitialized(product.getImages())
                    && !product.getImages().isEmpty()) {
                ProductImage img = product.getImages().get(0);
                if (img != null && img.getUrl() != null) return img.getUrl();
            }
        } catch (Exception ignored) {
            // LazyInitializationException – return null
        }
        return null;
    }

    private boolean isProductActive(Product product) {
        if (product == null) return false;
        return product.getStatus() == ProductStatus.ACTIVE && !product.isDeleted();
    }
}
