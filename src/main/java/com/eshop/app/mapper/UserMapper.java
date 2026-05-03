package com.eshop.app.mapper;

import com.eshop.app.dto.response.StoreInfoResponse;
import com.eshop.app.dto.response.UserResponse;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.enums.UserRole;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UserMapper {

        @Mapping(target = "role", source = "role")
    @Mapping(target = "shop", source = "store")
        @Mapping(target = "firstName", source = "userProfile.firstName")
        @Mapping(target = "lastName", source = "userProfile.lastName")
        @Mapping(target = "phone", source = "userProfile.phone")
        @Mapping(target = "address", expression = "java(getDefaultAddressLine1(user))")
        @Mapping(target = "addressLine1", expression = "java(getDefaultAddressLine1(user))")
        @Mapping(target = "addressLine2", expression = "java(getDefaultAddressLine2(user))")
        @Mapping(target = "city", expression = "java(getDefaultCity(user))")
        @Mapping(target = "district", expression = "java(getDefaultDistrict(user))")
        @Mapping(target = "state", expression = "java(getDefaultState(user))")
        @Mapping(target = "country", expression = "java(getDefaultCountry(user))")
        @Mapping(target = "pincode", expression = "java(getDefaultPincode(user))")
        @Mapping(target = "gender", source = "userProfile.gender")
        @Mapping(target = "dateOfBirth", source = "userProfile.dateOfBirth")
    UserResponse toUserResponse(User user);

    default String getDefaultAddressLine1(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getAddressLine1()
                                            : null;
    }

    default String getDefaultCity(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getCity()
                                            : null;
    }

    default String getDefaultAddressLine2(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getAddressLine2()
                                            : null;
    }

    default String getDefaultDistrict(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getDistrict()
                                            : null;
    }

    default String getDefaultState(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getState()
                                            : null;
    }

    default String getDefaultCountry(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getCountry()
                                            : null;
    }

    default String getDefaultPincode(User user) {
            return (user.getUserProfile() != null && user.getUserProfile().getAddresses() != null
                            && !user.getUserProfile().getAddresses().isEmpty())
                                            ? user.getUserProfile().getAddresses().get(0).getPincode()
                                            : null;
    }

    @Mapping(target = "storeName", source = "storeName")
    StoreInfoResponse toStoreInfoResponse(Store store);

    default String mapRole(UserRole role) {
            return role != null ? role.name() : null;
    }
}
