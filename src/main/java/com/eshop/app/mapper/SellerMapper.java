package com.eshop.app.mapper;

import com.eshop.app.dto.response.*;
import com.eshop.app.entity.*;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Dedicated mapper for {@link SellerProfile} → {@link SellerProfileResponse}.
 * <p>
 * Maps the core profile and all sub-entities (KYC, documents, bank accounts,
 * farmer details, business details, wholesale config) into a single response DTO.
 */
@Component
public class SellerMapper {

    /**
     * Convert a {@link SellerProfile} entity to its API response DTO.
     *
     * @param profile the seller profile entity (must not be null)
     * @return populated {@link SellerProfileResponse}
     */
    public SellerProfileResponse toResponse(SellerProfile profile) {
        // Fetch personal info from UserProfile via the shared user_id FK chain:
        // seller_profiles.user_id → users.id → user_profiles.user_id
        // No direct FK from seller_profiles to user_profiles is needed.
        UserProfile up = (profile.getUser() != null) ? profile.getUser().getUserProfile() : null;

        // Fetch store info (if exists)
        Store firstStore = (profile.getStores() != null && !profile.getStores().isEmpty())
                ? profile.getStores().iterator().next()
                : null;

        return SellerProfileResponse.builder()
                // Core IDs
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                // Personal Info (from UserProfile)
                .firstName(up != null ? up.getFirstName() : null)
                .lastName(up != null ? up.getLastName() : null)
                .profileImageUrl(up != null ? up.getProfileImageUrl() : null)
                .gender(up != null ? up.getGender() : null)
                .dateOfBirth(up != null ? up.getDateOfBirth() : null)
                .preferredLanguage(up != null ? up.getPreferredLanguage() : null)
                // Auth Info (from User)
                .email(profile.getUser() != null ? profile.getUser().getEmail() : null)
                .personalMobileNumber(up != null ? up.getPhone() : null)
                .businessMobileNumber(profile.getBusinessMobileNumber())
                // Seller / Business Info
                .identityType(profile.getIdentityType())
                .identityTypeLabel(profile.getIdentityType() != null ? profile.getIdentityType().getDisplayName() : null)
                .businessTypes(
                        profile.getBusinessTypes() != null
                                ? new HashSet<>(profile.getBusinessTypes())
                                : null)
                .shopName(profile.getShopName())
                .businessName(profile.getBusinessName())
                .description(profile.getDescription())
                .addressLine1(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getAddressLine1() : null)
                .addressLine2(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getAddressLine2() : null)
                .city(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getCity() : null)
                .district(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getDistrict() : null)
                .state(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getState() : null)
                .pincode(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getPincode() : null)
                .country(up != null && up.getAddresses() != null && !up.getAddresses().isEmpty() ? up.getAddresses().get(0).getCountry() : null)
                // Store Address (Prefer profile fields, fallback to Store entity if available)
                .storeAddressLine1(profile.getStoreAddressLine1() != null ? profile.getStoreAddressLine1() : (firstStore != null ? firstStore.getAddressLine1() : null))
                .storeAddressLine2(profile.getStoreAddressLine2() != null ? profile.getStoreAddressLine2() : (firstStore != null ? firstStore.getAddressLine2() : null))
                .storeCity(profile.getStoreCity() != null ? profile.getStoreCity() : (firstStore != null ? firstStore.getCity() : null))
                .storeDistrict(profile.getStoreDistrict() != null ? profile.getStoreDistrict() : (firstStore != null ? firstStore.getDistrict() : null))
                .storeState(profile.getStoreState() != null ? profile.getStoreState() : (firstStore != null ? firstStore.getState() : null))
                .storePincode(profile.getStorePincode() != null ? profile.getStorePincode() : (firstStore != null ? firstStore.getPostalCode() : null))
                .storeCountry(profile.getStoreCountry() != null ? profile.getStoreCountry() : (firstStore != null ? firstStore.getCountry() : null))
                .googleMapsUrl(profile.getGoogleMapsUrl() != null ? profile.getGoogleMapsUrl() : (firstStore != null ? firstStore.getGoogleMapsUrl() : null))
                .status(profile.getStatus())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .storeName(firstStore != null ? firstStore.getStoreName() : null)
                // Sub-entities
                .kyc(mapKyc(profile.getKyc()))
                .farmerDetails(mapFarmerDetails(profile.getFarmerDetails()))
                .businessDetails(mapBusinessDetails(profile.getBusinessDetails()))
                .wholesaleConfig(mapWholesaleConfig(profile.getWholesaleConfig()))
                .bankAccounts(profile.getBankAccounts() != null
                        ? profile.getBankAccounts().stream()
                                .map(this::mapBankAccount)
                                .collect(Collectors.toList())
                        : null)
                .documents(profile.getDocuments() != null
                        ? profile.getDocuments().stream()
                                .map(this::mapDocument)
                                .collect(Collectors.toList())
                        : null)
                // Audit
                .rejectionReason(profile.getRejectionReason())
                .approvedBy(profile.getApprovedBy())
                .approvedAt(profile.getApprovedAt())
                .build();
    }


    private SellerKYCResponse mapKyc(SellerKYC kyc) {
        if (kyc == null) return null;
        return SellerKYCResponse.builder()
                .id(kyc.getId())
                .panNumber(kyc.getPanNumber())
                .panName(kyc.getPanName())
                .gstin(kyc.getGstin())
                .gstRegistered(kyc.getGstRegistered())
                .businessType(kyc.getBusinessType())
                .verificationStatus(kyc.getVerificationStatus())
                .verifiedAt(kyc.getVerifiedAt())
                .verifiedBy(kyc.getVerifiedBy())
                .build();
    }

    private SellerFarmerDetailsResponse mapFarmerDetails(SellerFarmerDetails details) {
        if (details == null) return null;
        return SellerFarmerDetailsResponse.builder()
                .id(details.getId())
                .isOwnProduce(details.getIsOwnProduce())
                .farmLocation(details.getFarmLocation())
                .landArea(details.getLandArea())
                .cropTypes(details.getCropTypes())
                .build();
    }

    private SellerBusinessDetailsResponse mapBusinessDetails(SellerBusinessDetails details) {
        if (details == null) return null;
        return SellerBusinessDetailsResponse.builder()
                .id(details.getId())
                .legalBusinessName(details.getLegalBusinessName())
                .authorizedSignatory(details.getAuthorizedSignatory())
                .warehouseLocation(details.getWarehouseLocation())
                .build();
    }

    private SellerWholesaleConfigResponse mapWholesaleConfig(SellerWholesaleConfig config) {
        if (config == null) return null;
        return SellerWholesaleConfigResponse.builder()
                .id(config.getId())
                .bulkPricingEnabled(config.getBulkPricingEnabled())
                .minOrderQuantity(config.getMinOrderQuantity())
                .build();
    }

    private SellerBankAccountResponse mapBankAccount(SellerBankAccount ba) {
        return SellerBankAccountResponse.builder()
                .id(ba.getId())
                .accountHolderName(ba.getAccountHolderName())
                .accountNumber(ba.getAccountNumber())
                .ifscCode(ba.getIfscCode())
                .bankName(ba.getBankName())
                .isPrimary(ba.getIsPrimary())
                .verificationStatus(ba.getVerificationStatus())
                .build();
    }

    private SellerDocumentResponse mapDocument(SellerDocument doc) {
        return SellerDocumentResponse.builder()
                .id(doc.getId())
                .documentType(doc.getDocumentType())
                .documentNumber(doc.getDocumentNumber())
                .documentUrl(doc.getDocumentUrl())
                .verificationStatus(doc.getVerificationStatus())
                .verifiedAt(doc.getVerifiedAt())
                .verifiedBy(doc.getVerifiedBy())
                .rejectionReason(doc.getRejectionReason())
                .build();
    }
}
