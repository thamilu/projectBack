package com.eshop.app.mapper;

import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Store;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface StoreMapper {
    StoreMapper INSTANCE = Mappers.getMapper(StoreMapper.class);

    @Mapping(target = "sellerId", source = "sellerProfile.user.id")
    @Mapping(target = "sellerUsername", source = "sellerProfile.user.username")
    @Mapping(target = "pincode", source = "postalCode")
    @Mapping(target = "address", expression = "java(combineAddress(store))")
    StoreResponse toStoreResponse(Store store);

    default String combineAddress(Store store) {
        if (store == null) return null;
        StringBuilder sb = new StringBuilder();
        if (store.getAddressLine1() != null) sb.append(store.getAddressLine1());
        if (store.getAddressLine2() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getAddressLine2());
        }
        if (store.getCity() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getCity());
        }
        if (store.getState() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getState());
        }
        if (store.getPostalCode() != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(store.getPostalCode());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}
