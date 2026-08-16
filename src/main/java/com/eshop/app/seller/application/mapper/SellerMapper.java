package com.eshop.app.seller.application.mapper;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.api.response.SellerBankAccountResponse;
import com.eshop.app.seller.api.response.SellerBusinessDetailsResponse;
import com.eshop.app.seller.api.response.SellerDocumentResponse;
import com.eshop.app.seller.api.response.SellerFarmerDetailsResponse;
import com.eshop.app.seller.api.response.SellerKYCResponse;
import com.eshop.app.seller.api.response.SellerWholesaleConfigResponse;
import com.eshop.app.seller.domain.entity.SellerBankAccount;
import com.eshop.app.seller.domain.entity.SellerBusinessDetails;
import com.eshop.app.seller.domain.entity.SellerDocument;
import com.eshop.app.seller.domain.entity.SellerFarmerDetails;
import com.eshop.app.seller.domain.entity.SellerKYC;
import com.eshop.app.seller.domain.entity.SellerWholesaleConfig;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.user.api.request.SellerProfileUpdateRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.UserProfile;
import com.eshop.app.user.domain.entity.UserAddress;

import org.mapstruct.*;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Enterprise-grade MapStruct mapper for {@link SellerProfile}.
 *
 * <p>Automatically generates high-performance mapping code at compile time.
 *
 * <p><b>Design Notes:</b>
 * <ul>
 *   <li>{@code disableBuilder = true} is set globally — all mappings use setter-based strategy.
 *       {@link AfterMapping} therefore targets {@link SellerProfileResponse} directly, NOT its builder.</li>
 *   <li>Address resolution uses the first entry from {@code user.userProfile.addresses} as personal address.</li>
 *   <li>Store address resolution uses {@code SellerProfile.storeAddress*} fields, falling back to
 *       the first {@link Store}, then falling back to the personal address.</li>
 *   <li>{@link Set}-based list methods produce a stable sort by ID to ensure deterministic response ordering.</li>
 * </ul>
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        builder = @Builder(disableBuilder = true)
)
public interface SellerMapper {

    // =========================================================================
    // PRIMARY RESPONSE MAPPING
    // =========================================================================

    /**
     * Maps a {@link SellerProfile} to {@link SellerProfileResponse}.
     *
     * <p>Personal address fields are resolved from the first entry in
     * {@code user.userProfile.addresses}. Store address fields and display
     * identity are resolved via {@link #handleStoreFallbacksAndDerivedFields}.
     *
     * <p><b>Note:</b> {@code displayIdentity}, store address fields, and
     * {@code googleMapsUrl} are populated in the {@link AfterMapping} hook
     * to allow fallback logic. They are intentionally excluded here.
     */
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    // Personal info from UserProfile
    @Mapping(target = "firstName", source = "user.userProfile.firstName")
    @Mapping(target = "lastName", source = "user.userProfile.lastName")
    @Mapping(target = "profileImageUrl", source = "user.userProfile.profileImageUrl")
    @Mapping(target = "gender", source = ".", qualifiedByName = "resolveGender")
    @Mapping(target = "dateOfBirth", source = "user.userProfile.dateOfBirth")
    @Mapping(target = "preferredLanguage", source = "user.userProfile.preferredLanguage")
    @Mapping(target = "personalMobileNumber", source = "user.userProfile.phone")
    @Mapping(target = "alternatePhone", source = "user.userProfile.alternatePhone")
    // Identity label
    @Mapping(target = "identityTypeLabel", source = ".", qualifiedByName = "resolveIdentityTypeLabel")
    // Personal address — resolved from first address entry via @Named helpers
    @Mapping(target = "addressLine1", source = ".", qualifiedByName = "resolveAddressLine1")
    @Mapping(target = "city", source = ".", qualifiedByName = "resolvePersonalCity")
    @Mapping(target = "district", source = ".", qualifiedByName = "resolvePersonalDistrict")
    @Mapping(target = "taluk", source = ".", qualifiedByName = "resolvePersonalTaluk")
    @Mapping(target = "state", source = ".", qualifiedByName = "resolvePersonalState")
    @Mapping(target = "pincode", source = ".", qualifiedByName = "resolvePersonalPincode")
    // Store name from first store (non-determinism documented; stores are expected to be ordered by creation)
    @Mapping(target = "storeName", source = ".", qualifiedByName = "resolveStoreName")
    // Fields populated via @AfterMapping: displayIdentity, storeAddressLine1,
    // storeCity, storeDistrict, storeTaluk, storeState, storePincode, googleMapsUrl
    @Mapping(target = "displayIdentity", ignore = true)
    @Mapping(target = "storeAddressLine1", ignore = true)
    @Mapping(target = "storeCity", ignore = true)
    @Mapping(target = "storeDistrict", ignore = true)
    @Mapping(target = "storeTaluk", ignore = true)
    @Mapping(target = "storeState", ignore = true)
    @Mapping(target = "storePincode", ignore = true)
    @Mapping(target = "googleMapsUrl", ignore = true)
    SellerProfileResponse toResponse(SellerProfile profile);

    // =========================================================================
    // REGISTRATION MAPPING
    // =========================================================================

    /**
     * Applies fields from {@link SellerRegisterRequest} onto an existing {@link SellerProfile}.
     *
     * <p><b>Null-value policy:</b> {@code IGNORE} — prevents overwriting service-generated
     * fields (e.g. {@code shopHandle}) with null values from the request.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "businessMobileNumber", source = "businessPhone")
    @Mapping(target = "status", constant = "PENDING")
    void updateProfileFromRequest(SellerRegisterRequest request, @MappingTarget SellerProfile profile);

    // =========================================================================
    // PROFILE UPDATE MAPPING
    // =========================================================================

    /**
     * Applies partial updates from {@link SellerProfileUpdateRequest} onto an existing {@link SellerProfile}.
     *
     * <p>Audit and versioning fields are intentionally excluded from updates.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "businessMobileNumber", source = "businessPhone")
    void updateProfileFromUpdateRequest(SellerProfileUpdateRequest request, @MappingTarget SellerProfile profile);

    // =========================================================================
    // SUB-ENTITY RESPONSE MAPPINGS
    // =========================================================================

    /**
     * Maps {@link SellerKYC} to {@link SellerKYCResponse} with masked sensitive fields.
     *
     * <p>PAN is masked to show only the last 5 characters.
     * Aadhaar is masked to show only the last 4 digits.
     */
    @Mapping(target = "panNumber", source = "panNumber", qualifiedByName = "maskPan")
    @Mapping(target = "aadhar", source = "aadhar", qualifiedByName = "maskAadhar")
    SellerKYCResponse toKycResponse(SellerKYC kyc);

    SellerFarmerDetailsResponse toFarmerResponse(SellerFarmerDetails details);

    SellerBusinessDetailsResponse toBusinessResponse(SellerBusinessDetails details);

    SellerWholesaleConfigResponse toWholesaleResponse(SellerWholesaleConfig config);

    SellerBankAccountResponse toBankAccountResponse(SellerBankAccount ba);

    SellerDocumentResponse toDocumentResponse(SellerDocument doc);

    /**
     * Maps a {@link Set} of {@link SellerBankAccount} to a stable {@link List} of responses.
     *
     * <p>Sorted by {@code id} ascending to ensure deterministic ordering.
     * MapStruct delegates to {@link #toBankAccountResponse(SellerBankAccount)} per element.
     */
    default List<SellerBankAccountResponse> toBankAccountResponseList(Set<SellerBankAccount> set) {
        if (set == null) {
            return List.of();
        }
        return set.stream()
                .sorted(Comparator.comparing(SellerBankAccount::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toBankAccountResponse)
                .toList();
    }

    /**
     * Maps a {@link Set} of {@link SellerDocument} to a stable {@link List} of responses.
     *
     * <p>Sorted by {@code id} ascending to ensure deterministic ordering.
     * MapStruct delegates to {@link #toDocumentResponse(SellerDocument)} per element.
     */
    default List<SellerDocumentResponse> toDocumentResponseList(Set<SellerDocument> set) {
        if (set == null) {
            return List.of();
        }
        return set.stream()
                .sorted(Comparator.comparing(SellerDocument::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDocumentResponse)
                .toList();
    }

    // =========================================================================
    // AFTER MAPPING — Store Address Fallback & Derived Fields
    // =========================================================================

    /**
     * Resolves store address fields and the {@code displayIdentity} label after
     * primary mapping is complete.
     *
     * <p><b>Store address resolution priority:</b>
     * <ol>
     *   <li>{@code SellerProfile.storeAddress*} fields (explicitly set by seller)</li>
     *   <li>First {@link Store} entity address fields</li>
     *   <li>Personal address from {@code user.userProfile.addresses[0]}</li>
     * </ol>
     *
     * <p><b>Compatible with {@code disableBuilder = true}:</b> targets {@link SellerProfileResponse}
     * directly using setter-based strategy. Builder-based {@code @MappingTarget} would be silently
     * ignored when builders are disabled.
     */
    @AfterMapping
    default void handleStoreFallbacksAndDerivedFields(
            SellerProfile profile,
            @MappingTarget SellerProfileResponse response) {

        Store firstStore = resolveFirstStore(profile);
        UserAddress personalAddress = resolveFirstPersonalAddress(profile);

        // Store address fallback: profile fields → first store → personal address
        response.setStoreAddressLine1(resolveWithFallback(
                profile.getStoreAddressLine1(),
                firstStore != null ? firstStore.getAddressLine1() : null,
                personalAddress != null ? personalAddress.getAddressLine1() : null));

        response.setStoreCity(resolveWithFallback(
                profile.getStoreCity(),
                firstStore != null ? firstStore.getCity() : null,
                personalAddress != null ? personalAddress.getCity() : null));

        response.setStoreDistrict(resolveWithFallback(
                profile.getStoreDistrict(),
                firstStore != null ? firstStore.getDistrict() : null,
                personalAddress != null ? personalAddress.getDistrict() : null));

        response.setStoreTaluk(resolveWithFallback(
                profile.getStoreTaluk(),
                firstStore != null ? firstStore.getTaluk() : null,
                personalAddress != null ? personalAddress.getTaluk() : null));

        response.setStoreState(resolveWithFallback(
                profile.getStoreState(),
                firstStore != null ? firstStore.getState() : null,
                personalAddress != null ? personalAddress.getState() : null));

        response.setStorePincode(resolveWithFallback(
                profile.getStorePincode(),
                firstStore != null ? firstStore.getPostalCode() : null,
                personalAddress != null ? personalAddress.getPincode() : null));

        response.setGoogleMapsUrl(resolveWithFallback(
                profile.getGoogleMapsUrl(),
                firstStore != null ? firstStore.getGoogleMapsUrl() : null,
                null));

        // Display identity: "ShopName (City)" — null-safe with graceful degradation
        response.setDisplayIdentity(buildDisplayIdentity(profile.getShopName(), profile.getCity()));
    }

    // =========================================================================
    // NAMED HELPER METHODS — Address Resolution (eliminates DRY violation)
    // =========================================================================

    /**
     * Resolves the gender display name from the nested {@code user.userProfile.gender}.
     *
     * @param profile the seller profile
     * @return the gender enum name, or {@code null} if not set
     */
    @Named("resolveGender")
    default String resolveGender(SellerProfile profile) {
        if (profile == null || profile.getUser() == null) return null;
        UserProfile up = profile.getUser().getUserProfile();
        if (up == null || up.getGender() == null) return null;
        return up.getGender().name();
    }

    /**
     * Resolves the identity type display label.
     *
     * @param profile the seller profile
     * @return the display name, or {@code null} if not set
     */
    @Named("resolveIdentityTypeLabel")
    default String resolveIdentityTypeLabel(SellerProfile profile) {
        return (profile != null && profile.getIdentityType() != null)
                ? profile.getIdentityType().getDisplayName()
                : null;
    }

    /**
     * Resolves {@code addressLine1} from the first personal address.
     */
    @Named("resolveAddressLine1")
    default String resolveAddressLine1(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getAddressLine1() : null;
    }

    /**
     * Resolves {@code city} from the first personal address.
     */
    @Named("resolvePersonalCity")
    default String resolvePersonalCity(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getCity() : null;
    }

    /**
     * Resolves {@code district} from the first personal address.
     */
    @Named("resolvePersonalDistrict")
    default String resolvePersonalDistrict(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getDistrict() : null;
    }

    /**
     * Resolves {@code taluk} from the first personal address.
     */
    @Named("resolvePersonalTaluk")
    default String resolvePersonalTaluk(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getTaluk() : null;
    }

    /**
     * Resolves {@code state} from the first personal address.
     */
    @Named("resolvePersonalState")
    default String resolvePersonalState(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getState() : null;
    }

    /**
     * Resolves {@code pincode} from the first personal address.
     */
    @Named("resolvePersonalPincode")
    default String resolvePersonalPincode(SellerProfile profile) {
        UserAddress addr = resolveFirstPersonalAddress(profile);
        return addr != null ? addr.getPincode() : null;
    }

    /**
     * Resolves the store name from the first store in the profile's store set.
     *
     * <p>Store ordering within a {@link Set} is non-deterministic. The result
     * reflects whichever store the JVM iterator returns first. If ordering matters,
     * use a {@link java.util.SortedSet} or apply explicit ordering at the service layer.
     */
    @Named("resolveStoreName")
    default String resolveStoreName(SellerProfile profile) {
        Store store = resolveFirstStore(profile);
        return store != null ? store.getStoreName() : null;
    }

    // =========================================================================
    // MASKING METHODS — Sensitive Data Protection
    // =========================================================================

    /**
     * Masks a PAN number, showing only the last 5 characters.
     *
     * <p>Example: {@code ABCDE1234F} → {@code XXXXX1234F}
     *
     * @param pan the full PAN number, or {@code null}
     * @return the masked PAN, or {@code null} if input is {@code null}
     */
    @Named("maskPan")
    default String maskPan(String pan) {
        if (pan == null || pan.length() < 5) {
            return pan;
        }
        return "X".repeat(pan.length() - 5) + pan.substring(pan.length() - 5);
    }

    /**
     * Masks an Aadhaar number, showing only the last 4 digits with formatting.
     *
     * <p>Example: {@code 123456781234} → {@code XXXX-XXXX-1234}
     *
     * @param aadhar the full Aadhaar number, or {@code null}
     * @return the masked Aadhaar, or {@code null} if input is {@code null}
     */
    @Named("maskAadhar")
    default String maskAadhar(String aadhar) {
        if (aadhar == null || aadhar.length() < 4) {
            return aadhar;
        }
        String digits = aadhar.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return aadhar;
        }
        return "XXXX-XXXX-" + digits.substring(digits.length() - 4);
    }

    // =========================================================================
    // PRIVATE UTILITY METHODS
    // =========================================================================

    /**
     * Resolves the first personal {@link UserAddress} from the profile's nested user profile.
     *
     * <p>Returns {@code null} if any required node in the chain is absent.
     *
     * @param profile the seller profile
     * @return the first address, or {@code null}
     */
    private UserAddress resolveFirstPersonalAddress(SellerProfile profile) {
        if (profile == null || profile.getUser() == null) return null;
        UserProfile up = profile.getUser().getUserProfile();
        if (up == null || up.getAddresses() == null || up.getAddresses().isEmpty()) return null;
        return up.getAddresses().get(0);
    }

    /**
     * Resolves the first {@link Store} from the profile's store set.
     *
     * <p>Returns {@code null} if the store set is null or empty.
     *
     * @param profile the seller profile
     * @return the first store iterated, or {@code null}
     */
    private Store resolveFirstStore(SellerProfile profile) {
        if (profile == null || profile.getStores() == null || profile.getStores().isEmpty()) {
            return null;
        }
        return profile.getStores().iterator().next();
    }

    /**
     * Returns the first non-null value among the provided candidates in priority order.
     *
     * @param primary   highest priority value
     * @param secondary fallback value
     * @param tertiary  last-resort value
     * @return first non-null candidate, or {@code null} if all are null
     */
    private String resolveWithFallback(String primary, String secondary, String tertiary) {
        if (primary != null) return primary;
        if (secondary != null) return secondary;
        return tertiary;
    }

    /**
     * Builds the display identity label in the format {@code "ShopName (City)"}.
     *
     * <p>Gracefully handles null inputs:
     * <ul>
     *   <li>If both are null → returns {@code null}</li>
     *   <li>If only {@code shopName} is null → returns the city</li>
     *   <li>If only {@code city} is null → returns the shop name</li>
     *   <li>If both are present → returns {@code "ShopName (City)"}</li>
     * </ul>
     *
     * @param shopName the seller's shop name
     * @param city     the resolved store city
     * @return the formatted display identity, or a graceful degradation
     */
    private String buildDisplayIdentity(String shopName, String city) {
        if (shopName == null && city == null) return null;
        if (shopName == null) return city;
        if (city == null) return shopName;
        return shopName + " (" + city + ")";
    }
}
