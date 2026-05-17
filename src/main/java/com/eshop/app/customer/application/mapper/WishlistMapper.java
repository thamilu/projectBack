package com.eshop.app.customer.application.mapper;

import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductImage;
import com.eshop.app.catalog.domain.entity.ProductStatus;
import com.eshop.app.customer.api.response.WishlistResponse;
import com.eshop.app.customer.domain.entity.Wishlist;
import com.eshop.app.core.api.response.PageResponse;

import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Dedicated mapper for {@link Wishlist} â†’ {@link WishlistResponse}.
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
     */
    public PageResponse<WishlistResponse> toPageResponse(Page<Wishlist> page) {
        return PageResponse.of(page, this::toResponse);
    }

    // â”€â”€â”€ private helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
            // LazyInitializationException â€“ return null
        }
        return null;
    }

    private boolean isProductActive(Product product) {
        if (product == null) return false;
        return product.getStatus() == ProductStatus.ACTIVE && !product.isDeleted();
    }
}


