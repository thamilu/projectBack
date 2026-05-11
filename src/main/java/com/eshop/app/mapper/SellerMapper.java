package com.eshop.app.mapper;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.response.*;
import com.eshop.app.entity.*;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;

/**
 * Enterprise-grade MapStruct mapper for {@link SellerProfile}.
 * Automatically generates high-performance mapping code at compile time.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder(disableBuilder = true))
public interface SellerMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    // Personal Info from UserProfile
    @Mapping(target = "firstName", source = "user.userProfile.firstName")
    @Mapping(target = "lastName", source = "user.userProfile.lastName")
    @Mapping(target = "profileImageUrl", source = "user.userProfile.profileImageUrl")
    @Mapping(target = "gender", source = "user.userProfile.gender")
    @Mapping(target = "dateOfBirth", source = "user.userProfile.dateOfBirth")
    @Mapping(target = "preferredLanguage", source = "user.userProfile.preferredLanguage")
    @Mapping(target = "personalMobileNumber", source = "user.userProfile.phone")
    @Mapping(target = "alternatePhone", source = "user.userProfile.alternatePhone")
    // Identity label
    @Mapping(target = "identityTypeLabel", expression = "java(profile.getIdentityType() != null ? profile.getIdentityType().getDisplayName() : null)")
    // Address Fallbacks logic handled via custom after-mapping if needed, but simple ones here:
    @Mapping(target = "addressLine1", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getAddressLine1() : null)")
    @Mapping(target = "city", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getCity() : null)")
    @Mapping(target = "district", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getDistrict() : null)")
    @Mapping(target = "taluk", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getTaluk() : null)")
    @Mapping(target = "state", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getState() : null)")
    @Mapping(target = "pincode", expression = "java(profile.getUser() != null && profile.getUser().getUserProfile() != null && !profile.getUser().getUserProfile().getAddresses().isEmpty() ? profile.getUser().getUserProfile().getAddresses().get(0).getPincode() : null)")
    // Store logic
    @Mapping(target = "storeName", expression = "java(profile.getStores() != null && !profile.getStores().isEmpty() ? profile.getStores().iterator().next().getStoreName() : null)")
    // Global Identity
    @Mapping(target = "displayIdentity", expression = "java(profile.getShopName() + \" (\" + profile.getCity() + \")\")")
    SellerProfileResponse toResponse(SellerProfile profile);

    @Mapping(target = "businessMobileNumber", source = "businessPhone")
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "shopHandle", source = "shopHandle") // Handled by service if null
    @Mapping(target = "storeAddressLine1", source = "storeAddressLine1")
    @Mapping(target = "storeAddressLine2", source = "storeAddressLine2")
    @Mapping(target = "storeCity", source = "storeCity")
    @Mapping(target = "storeDistrict", source = "storeDistrict")
    @Mapping(target = "storeTaluk", source = "storeTaluk")
    @Mapping(target = "storeState", source = "storeState")
    @Mapping(target = "storePincode", source = "storePincode")
    @Mapping(target = "storeCountry", source = "storeCountry")
    @Mapping(target = "googleMapsUrl", source = "googleMapsUrl")
    void updateProfileFromRequest(SellerRegisterRequest request, @MappingTarget SellerProfile profile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "businessMobileNumber", source = "businessPhone")
    void updateProfileFromUpdateRequest(SellerProfileUpdateRequest request, @MappingTarget SellerProfile profile);

    SellerKYCResponse toKycResponse(SellerKYC kyc);

    SellerFarmerDetailsResponse toFarmerResponse(SellerFarmerDetails details);

    SellerBusinessDetailsResponse toBusinessResponse(SellerBusinessDetails details);

    SellerWholesaleConfigResponse toWholesaleResponse(SellerWholesaleConfig config);

    SellerBankAccountResponse toBankAccountResponse(SellerBankAccount ba);

    SellerDocumentResponse toDocumentResponse(SellerDocument doc);

    List<SellerBankAccountResponse> toBankAccountResponseList(Set<SellerBankAccount> set);

    List<SellerDocumentResponse> toDocumentResponseList(Set<SellerDocument> set);

    @AfterMapping
    default void handleStoreFallbacks(SellerProfile profile, @MappingTarget SellerProfileResponse.SellerProfileResponseBuilder response) {
        Store firstStore = (profile.getStores() != null && !profile.getStores().isEmpty())
                ? profile.getStores().iterator().next()
                : null;

        response.storeAddressLine1(profile.getStoreAddressLine1() != null ? profile.getStoreAddressLine1() : (firstStore != null ? firstStore.getAddressLine1() : profile.getAddressLine1()));
        response.storeCity(profile.getStoreCity() != null ? profile.getStoreCity() : (firstStore != null ? firstStore.getCity() : profile.getCity()));
        response.storeDistrict(profile.getStoreDistrict() != null ? profile.getStoreDistrict() : (firstStore != null ? firstStore.getDistrict() : profile.getDistrict()));
        response.storeTaluk(profile.getStoreTaluk() != null ? profile.getStoreTaluk() : (firstStore != null ? firstStore.getTaluk() : profile.getTaluk()));
        response.storeState(profile.getStoreState() != null ? profile.getStoreState() : (firstStore != null ? firstStore.getState() : profile.getState()));
        response.storePincode(profile.getStorePincode() != null ? profile.getStorePincode() : (firstStore != null ? firstStore.getPostalCode() : profile.getPincode()));
        response.googleMapsUrl(profile.getGoogleMapsUrl() != null ? profile.getGoogleMapsUrl() : (firstStore != null ? firstStore.getGoogleMapsUrl() : null));
    }
}
