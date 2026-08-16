package com.eshop.app.media.application.mapper;

import com.eshop.app.media.api.response.MediaAssetResponse;
import com.eshop.app.media.domain.entity.MediaAsset;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** MapStruct mapper for MediaAsset entity → response DTO conversion. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaAssetMapper {

    @Mapping(target = "uploadStatus", expression = "java(entity.getUploadStatus().name())")
    MediaAssetResponse toResponse(MediaAsset entity);

    List<MediaAssetResponse> toResponseList(List<MediaAsset> entities);
}
