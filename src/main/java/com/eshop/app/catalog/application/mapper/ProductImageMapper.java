package com.eshop.app.catalog.application.mapper;

import com.eshop.app.catalog.api.response.ProductImageResponse;
import com.eshop.app.catalog.domain.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductImageMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "imageUrl", source = "url")
    @Mapping(target = "displayOrder", source = "sortOrder")
    ProductImageResponse toProductImageResponse(ProductImage image);
}
