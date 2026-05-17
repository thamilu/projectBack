package com.eshop.app.catalog.application.mapper;

import com.eshop.app.catalog.api.response.CategoryResponse;
import com.eshop.app.catalog.domain.entity.Category;




import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryMapper INSTANCE = Mappers.getMapper(CategoryMapper.class);

    CategoryResponse toCategoryResponse(Category category);
}
