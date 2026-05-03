package com.eshop.app.mapper;

import com.eshop.app.dto.request.BrandRequest;
import com.eshop.app.dto.response.BrandDetailResponse;
import com.eshop.app.dto.response.BrandResponse;
import com.eshop.app.dto.response.BrandSummaryResponse;
import com.eshop.app.entity.Brand;
import org.mapstruct.*;



@Mapper(
    config = com.eshop.app.config.MapStructConfig.class,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface BrandMapper {


    @Mappings({
        @Mapping(target = "deleted", ignore = true),
        @Mapping(target = "deletedAt", ignore = true),
        @Mapping(target = "products", ignore = true),
        @Mapping(target = "seoMetadata", ignore = true)
    })
    Brand toEntity(BrandRequest request);


    @Mappings({
        @Mapping(target = "metaTitle", source = "seoMetadata.metaTitle"),
        @Mapping(target = "metaDescription", source = "seoMetadata.metaDescription")
    })
    BrandResponse toResponse(Brand brand);


    @Mappings({
        @Mapping(target = "metaTitle", source = "seoMetadata.metaTitle"),
        @Mapping(target = "metaDescription", source = "seoMetadata.metaDescription")
    })
    BrandDetailResponse toDetailResponse(Brand brand);


    @Mappings({
        @Mapping(target = "id", source = "id"),
        @Mapping(target = "name", source = "name"),
        @Mapping(target = "slug", source = "slug"),
        @Mapping(target = "logoUrl", source = "logoUrl")
    })
    BrandSummaryResponse toSummaryResponse(Brand brand);


    @Mappings({
        @Mapping(target = "deleted", ignore = true),
        @Mapping(target = "deletedAt", ignore = true),
        @Mapping(target = "products", ignore = true),
        @Mapping(target = "seoMetadata", ignore = true)
    })
    void updateEntity(@MappingTarget Brand brand, BrandRequest request);
}
