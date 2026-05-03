package com.eshop.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.eshop.app.enums.KycBusinessType;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerBusinessType;
import java.util.Set;

/**
 * Step-wise seller onboarding request.
 * <p>
 * Step 1 (Basic): identityType, shopName
 * Step 2 (KYC): panNumber (mandatory), gstin (conditional)
 * Step 3 (Bank): accountNumber, ifscCode
 * Step 4 (Optional): farmerDetails, businessDetails, wholesaleConfig
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegisterRequest {

    // ─── Step 1: Basic Identity ─────────────────────────────────

    @NotNull(message = "Seller identity type is required")
    private SellerIdentityType identityType;

    @NotEmpty(message = "At least one business type is required")
    private Set<SellerBusinessType> businessTypes;

    @NotBlank(message = "Shop name is required")
    @Size(min = 2, max = 200, message = "Shop name must be between 2 and 200 characters")
    private String shopName;

    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;

    @Size(min = 2, max = 250, message = "Shop handle must be between 2 and 250 characters")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Shop handle must be lowercase alphanumeric with hyphens")
    private String shopHandle;

    private String shopLogoUrl;

    private String firstName;
    private String lastName;

    @NotBlank(message = "Personal phone number is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 20, message = "Alternate phone must not exceed 20 characters")
    private String alternatePhone;

    @Size(max = 20, message = "Gender must not exceed 20 characters")
    private String gender;

    @Size(max = 20, message = "Preferred language must not exceed 20 characters")
    private String preferredLanguage;

    private java.time.LocalDate dateOfBirth;

    @NotBlank(message = "Business phone number is required")
    @Size(max = 20, message = "Business Phone must not exceed 20 characters")
    private String businessPhone;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @AssertTrue(message = "Terms must be accepted")
    private boolean acceptedTerms;

    // ─── Step 2: KYC ────────────────────────────────────────────

    @Size(max = 20, message = "PAN number must not exceed 20 characters")
    private String panNumber;

    @Size(max = 150, message = "PAN name must not exceed 150 characters")
    private String panName;

    @Pattern(regexp = "^([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})?$", message = "Invalid GSTIN format")
    private String gstin;

    private Boolean gstRegistered;

    private KycBusinessType kycBusinessType;

    // ─── Step 2b: Documents ─────────────────────────────────────

    private String aadhar;
    private String registrationProof;

    // ─── Step 3: Bank Details ───────────────────────────────────

    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;

    // ─── Step 4: Optional Modules ───────────────────────────────

    // Farmer Details (required if identity_type = FARMER)
    private Boolean isOwnProduce;
    private String farmLocationVillage;
    private String landArea;
    private String cropTypes;

    // Business Details (required if identity_type = BUSINESS)
    private String legalBusinessName;
    private String authorizedSignatory;
    private String warehouseLocation;

    // Wholesale Config
    private Boolean bulkPricingEnabled;
    private Integer minOrderQuantity;

    // ─── Step 5: Address Details ───────────────────────────────
    @NotBlank(message = "Address Line 1 is required")
    private String addressLine1;
    
    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;
    
    private String district;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotBlank(message = "Country is required")
    private String country;

    // Store / Warehouse Address (Distinct from personal address)
    private String storeAddressLine1;
    private String storeAddressLine2;
    private String storeCity;
    private String storeDistrict;
    private String storeState;
    private String storePincode;
    private String storeCountry;
    private String googleMapsUrl;

    // ─── Legacy Compatibility (Do not use in new implementations) ───
}
