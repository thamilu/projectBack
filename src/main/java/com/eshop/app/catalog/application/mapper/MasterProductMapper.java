package com.eshop.app.catalog.application.mapper;

import com.eshop.app.catalog.api.response.MasterProductResponse;
import com.eshop.app.catalog.domain.entity.MasterProduct;
import com.eshop.app.catalog.domain.entity.ProductMedia;
import java.util.List;
import org.mapstruct.*;

@Mapper(
        config = com.eshop.app.core.infrastructure.config.mapper.MapStructConfig.class,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MasterProductMapper {

    @Mappings({
        @Mapping(target = "brandId", source = "brand.id"),
        @Mapping(target = "brandName", source = "brand.name"),
        @Mapping(target = "categoryId", source = "category.id"),
        @Mapping(target = "categoryName", source = "category.name"),
        @Mapping(target = "parentMasterProductId", source = "parentMasterProduct.id"),
        @Mapping(target = "rootMasterProductId", source = "rootMasterProduct.id"),
        @Mapping(
                target = "productType",
                expression =
                        "java(mp.getProductType() != null ? mp.getProductType().name() : null)"),
        @Mapping(
                target = "imageUrl",
                expression =
                        "java(com.eshop.app.core.util.MediaUtils.extractPrimaryImageUrl(mp.getMedia()))"),
        @Mapping(target = "media", source = "media")
    })
    MasterProductResponse toResponse(MasterProduct mp);

    List<MasterProductResponse> toResponses(List<MasterProduct> mps);

    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "mediaUrl", source = "mediaUrl"),
        @Mapping(target = "mediaType", source = "mediaType"),
        @Mapping(target = "isPrimary", source = "isPrimary"),
        @Mapping(target = "sortOrder", source = "sortOrder"),
        @Mapping(target = "altText", source = "altText")
    })
    MasterProductResponse.MediaResponse toMediaResponse(ProductMedia pm);

    List<MasterProductResponse.MediaResponse> toMediaResponses(List<ProductMedia> media);
}
