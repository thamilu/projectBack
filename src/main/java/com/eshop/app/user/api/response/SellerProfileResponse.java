package com.eshop.app.user.api.response;

import com.eshop.app.seller.api.response.SellerBankAccountResponse;
import com.eshop.app.seller.api.response.SellerBusinessDetailsResponse;
import com.eshop.app.seller.api.response.SellerDocumentResponse;
import com.eshop.app.seller.api.response.SellerFarmerDetailsResponse;
import com.eshop.app.seller.api.response.SellerKYCResponse;
import com.eshop.app.seller.api.response.SellerWholesaleConfigResponse;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerProfileResponse {

    // ─── Core IDs ───────────────────────────────────────────────
    private Long id;
    private Long userId;

    // ─── Personal Info (from UserProfile via user_id → user → user_profile) ──
    /** First name stored in user_profiles.first_name */
    private String firstName;
    /** Last name stored in user_profiles.last_name */
    private String lastName;
    private String profileImageUrl;
    private String gender;
    private LocalDate dateOfBirth;
    private String preferredLanguage;

    // ─── Auth Info (from users table) ───────────────────────────
    private String email;
    /** Personal phone stored in user_profiles.phone (for 2FA/security) */
    private String personalMobileNumber;

    /** Alternate phone stored in user_profiles.alternate_phone */
    private String alternatePhone;

    /**
     * Business/Support phone stored in seller_profiles.businessMobileNumber (shown
     * to customers)
     */
    private String businessMobileNumber;

    // ─── Seller / Business Info (from seller_profiles) ──────────
    private SellerIdentityType identityType;
    private String identityTypeLabel;
    private Set<SellerBusinessType> businessTypes;
    private String shopName;
    private String shopHandle;
    private String shopLogoUrl;
    /** Coinputed field: "ShopName (City)" for zero confusion */
    private String displayIdentity;

    private String businessName;
    private String description;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String taluk;
    private String state;
    private String pincode;
    private String country;

    // Store / Warehouse Address
    private String storeAddressLine1;
    private String storeAddressLine2;
    private String storeCity;
    private String storeDistrict;
    private String storeTaluk;
    private String storeState;
    private String storePincode;
    private String storeCountry;

    private String googleMapsUrl;
    private SellerStatus status;

    // ─── Timestamps ─────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Store info ─────────────────────────────────────────────
    private String storeName;

    // ─── Nested sub-entity responses ────────────────────────────
    private SellerKYCResponse kyc;
    private SellerFarmerDetailsResponse farmerDetails;
    private SellerBusinessDetailsResponse businessDetails;
    private SellerWholesaleConfigResponse wholesaleConfig;
    private List<SellerBankAccountResponse> bankAccounts;
    private List<SellerDocumentResponse> documents;

    // ─── Audit ──────────────────────────────────────────────────
    private String rejectionReason;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
