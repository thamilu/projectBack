package com.eshop.app.mapper;

import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Store;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
public interface StoreMapper {
    StoreMapper INSTANCE = Mappers.getMapper(StoreMapper.class);

    @Mapping(target = "sellerId", source = "sellerProfile.user.id")
    @Mapping(target = "sellerEmail", source = "sellerProfile.user.email")
    @Mapping(target = "shopHandle", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getShopHandle()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getShopHandle() : null) : store.getShopHandle())")
    @Mapping(target = "pincode", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getPostalCode()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStorePincode() : null) : store.getPostalCode())")
    @Mapping(target = "isVerified", expression = "java(store.getSellerProfile() != null && store.getSellerProfile().getStatus() == com.eshop.app.enums.SellerStatus.ACTIVE)")
    @Mapping(target = "totalRatings", constant = "0L")
    @Mapping(target = "city", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getCity()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStoreCity() : null) : store.getCity())")
    @Mapping(target = "state", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getState()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStoreState() : null) : store.getState())")
    @Mapping(target = "country", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getCountry()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStoreCountry() : \"India\") : store.getCountry())")
    @Mapping(target = "district", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getDistrict()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStoreDistrict() : null) : store.getDistrict())")
    @Mapping(target = "taluk", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(store.getTaluk()) ? (store.getSellerProfile() != null ? store.getSellerProfile().getStoreTaluk() : null) : store.getTaluk())")
    @Mapping(target = "address", expression = "java(combineAddress(store))")
    StoreResponse toStoreResponse(Store store);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "products", ignore = true)
    @Mapping(target = "deleted", constant = "false")
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "sellerProfile", source = "profile")
    @Mapping(target = "storeName", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getShopName()) ? profile.getUser().getEmail() + \"'s Store\" : profile.getShopName())")
    @Mapping(target = "description", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getDescription()) ? \"Welcome to \" + (org.apache.commons.lang3.StringUtils.isBlank(profile.getShopName()) ? profile.getUser().getEmail() + \"'s Store\" : profile.getShopName()) : profile.getDescription())")
    @Mapping(target = "phone", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getBusinessMobileNumber()) ? (profile.getUser().getUserProfile() != null ? profile.getUser().getUserProfile().getPhone() : null) : profile.getBusinessMobileNumber())")
    @Mapping(target = "email", source = "profile.user.email")
    @Mapping(target = "logoUrl", source = "profile.shopLogoUrl")
    @Mapping(target = "shopHandle", source = "profile.shopHandle")
    @Mapping(target = "addressLine1", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreAddressLine1()) ? profile.getAddressLine1() : profile.getStoreAddressLine1())")
    @Mapping(target = "addressLine2", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreAddressLine2()) ? profile.getAddressLine2() : profile.getStoreAddressLine2())")
    @Mapping(target = "city", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreCity()) ? profile.getCity() : profile.getStoreCity())")
    @Mapping(target = "district", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreDistrict()) ? profile.getDistrict() : profile.getStoreDistrict())")
    @Mapping(target = "taluk", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreTaluk()) ? profile.getTaluk() : profile.getStoreTaluk())")
    @Mapping(target = "state", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreState()) ? profile.getState() : profile.getStoreState())")
    @Mapping(target = "country", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStoreCountry()) ? profile.getCountry() : profile.getStoreCountry())")
    @Mapping(target = "postalCode", expression = "java(org.apache.commons.lang3.StringUtils.isBlank(profile.getStorePincode()) ? profile.getPincode() : profile.getStorePincode())")
    @Mapping(target = "googleMapsUrl", source = "profile.googleMapsUrl")
    Store toStore(com.eshop.app.entity.SellerProfile profile);

    default boolean syncMissingData(com.eshop.app.entity.Store store, com.eshop.app.entity.SellerProfile profile) {
        if (profile == null || store == null) return false;
        boolean changed = false;
        
        if (isBlank(store.getAddressLine1()) && !isBlank(profile.getStoreAddressLine1())) { store.setAddressLine1(profile.getStoreAddressLine1()); changed = true; }
        else if (isBlank(store.getAddressLine1()) && !isBlank(profile.getAddressLine1())) { store.setAddressLine1(profile.getAddressLine1()); changed = true; }
        
        if (isBlank(store.getCity()) && !isBlank(profile.getStoreCity())) { store.setCity(profile.getStoreCity()); changed = true; }
        else if (isBlank(store.getCity()) && !isBlank(profile.getCity())) { store.setCity(profile.getCity()); changed = true; }
        
        if (isBlank(store.getState()) && !isBlank(profile.getStoreState())) { store.setState(profile.getStoreState()); changed = true; }
        else if (isBlank(store.getState()) && !isBlank(profile.getState())) { store.setState(profile.getState()); changed = true; }

        if (isBlank(store.getDistrict()) && !isBlank(profile.getStoreDistrict())) { store.setDistrict(profile.getStoreDistrict()); changed = true; }
        else if (isBlank(store.getDistrict()) && !isBlank(profile.getDistrict())) { store.setDistrict(profile.getDistrict()); changed = true; }

        if (isBlank(store.getTaluk()) && !isBlank(profile.getStoreTaluk())) { store.setTaluk(profile.getStoreTaluk()); changed = true; }
        else if (isBlank(store.getTaluk()) && !isBlank(profile.getTaluk())) { store.setTaluk(profile.getTaluk()); changed = true; }
        
        if (isBlank(store.getShopHandle()) && !isBlank(profile.getShopHandle())) { store.setShopHandle(profile.getShopHandle()); changed = true; }
        if (isBlank(store.getLogoUrl()) && !isBlank(profile.getShopLogoUrl())) { store.setLogoUrl(profile.getShopLogoUrl()); changed = true; }
        if (isBlank(store.getPhone()) && !isBlank(profile.getBusinessMobileNumber())) { store.setPhone(profile.getBusinessMobileNumber()); changed = true; }
        
        if (isBlank(store.getPostalCode()) && !isBlank(profile.getStorePincode())) { store.setPostalCode(profile.getStorePincode()); changed = true; }
        else if (isBlank(store.getPostalCode()) && !isBlank(profile.getPincode())) { store.setPostalCode(profile.getPincode()); changed = true; }

        if (!isBlank(profile.getDescription()) && (isBlank(store.getDescription()) || store.getDescription().startsWith("Welcome to "))) {
            store.setDescription(profile.getDescription());
            changed = true;
        }
        return changed;
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    default String combineAddress(Store store) {
        if (store == null) return null;
        
        // Use fallbacks from SellerProfile if store fields are blank
        com.eshop.app.entity.SellerProfile profile = store.getSellerProfile();
        
        String addr1 = !isBlank(store.getAddressLine1()) ? store.getAddressLine1() : (profile != null ? profile.getStoreAddressLine1() : null);
        String addr2 = !isBlank(store.getAddressLine2()) ? store.getAddressLine2() : (profile != null ? profile.getStoreAddressLine2() : null);
        String city = !isBlank(store.getCity()) ? store.getCity() : (profile != null ? profile.getStoreCity() : null);
        String taluk = !isBlank(store.getTaluk()) ? store.getTaluk() : (profile != null ? profile.getStoreTaluk() : null);
        String state = !isBlank(store.getState()) ? store.getState() : (profile != null ? profile.getStoreState() : null);
        String pincode = !isBlank(store.getPostalCode()) ? store.getPostalCode() : (profile != null ? profile.getStorePincode() : null);
        String country = profile != null ? profile.getStoreCountry() : "India"; // Default to India if not specified

        StringBuilder sb = new StringBuilder();
        if (!isBlank(addr1)) sb.append(addr1);
        if (!isBlank(addr2)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(addr2);
        }
        if (!isBlank(city)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (!isBlank(taluk)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(taluk);
        }
        if (!isBlank(state)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (!isBlank(country)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(country);
        }
        if (!isBlank(pincode)) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(pincode);
        }
        
        return sb.length() > 0 ? sb.toString() : null;
    }
}
