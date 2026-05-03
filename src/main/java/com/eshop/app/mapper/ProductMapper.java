package com.eshop.app.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.eshop.app.dto.response.ProductResponse;
import com.eshop.app.dto.response.ProductListResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.ProductReview;
import com.eshop.app.entity.Tag;
import com.eshop.app.entity.enums.ProductStatus;
import com.eshop.app.repository.projection.ProductSummaryProjection;
import org.hibernate.Hibernate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    /* ========= ENTITY → DTO ========= */

    @Mapping(target = "tags", expression = "java(mapTags(product.getTags()))")
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "active", expression = "java(isProductActive(product))")
    @Mapping(target = "categoryAttributes", source = "attributes")
    @Mapping(target = "categoryType", ignore = true)
    @Mapping(target = "subCategory", ignore = true)
    @Mapping(target = "imageUrl", expression = "java(getPrimaryImageUrl(product))")
    ProductResponse toProductResponse(Product product);

    ProductResponse toProductResponse(ProductSummaryProjection summary);

    /**
     * Map projection to lightweight list response.
     * Used for GET /products list endpoints.
     */
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "storeName", ignore = true)
    ProductListResponse toProductListResponse(ProductSummaryProjection summary);

    /**
     * Map full entity to lightweight list response.
     * Used when projection queries are not available.
     */
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "storeName", source = "store.storeName")
    @Mapping(target = "imageUrl", expression = "java(getPrimaryImageUrl(product))")
    @Mapping(target = "active", expression = "java(isProductActive(product))")
    ProductListResponse toProductListResponseFromEntity(Product product);

    /* ========= CUSTOM MAPPINGS ========= */

    default List<String> mapTags(Set<Tag> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream()
                .map(Tag::getName)
                .toList();
    }

    // images mapping ignored — handled elsewhere if needed

    default Double mapAverageRating(Set<ProductReview> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(ProductReview::getRating)
                .average()
                .orElse(0.0);
    }

    default String getPrimaryImageUrl(Product product) {
        if (product == null) {
            return null;
        }
        // Use primary image if available, otherwise use images collection
        if (product.getPrimaryImage() != null) {
            return product.getPrimaryImage().getUrl();
        }
        // Safely check images collection initialization to avoid LazyInitializationException
        try {
            if (product.getImages() != null && Hibernate.isInitialized(product.getImages()) && !product.getImages().isEmpty()) {
                com.eshop.app.entity.ProductImage img = product.getImages().get(0);
                if (img != null && img.getUrl() != null) {
                    return img.getUrl();
                }
            }
        } catch (Exception e) {
            // Swallow and return null to avoid crashing serialization when session is closed
        }
        return null;
    }

    default boolean isProductActive(Product product) {
        if (product == null) {
            return false;
        }
        return product.getStatus() == ProductStatus.ACTIVE && !product.isDeleted();
    }
}


