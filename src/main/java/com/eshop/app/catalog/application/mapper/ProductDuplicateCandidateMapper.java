package com.eshop.app.catalog.application.mapper;

import com.eshop.app.catalog.api.response.ProductDuplicateCandidateResponse;
import com.eshop.app.catalog.domain.entity.ProductDuplicateCandidate;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(config = com.eshop.app.core.infrastructure.config.mapper.MapStructConfig.class)
public interface ProductDuplicateCandidateMapper {

    ProductDuplicateCandidateResponse toResponse(ProductDuplicateCandidate candidate);

    List<ProductDuplicateCandidateResponse> toResponseList(
            List<ProductDuplicateCandidate> candidates);
}
