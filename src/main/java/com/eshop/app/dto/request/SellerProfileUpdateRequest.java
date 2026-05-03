package com.eshop.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.eshop.app.enums.KycBusinessType;
import java.time.LocalDate;

/**
 * Seller profile update request — supports partial updates to all sub-entities.
 * <p>
 * Personal info fields (firstName, lastName, phone, etc.) are written to
 * {@code user_profiles} via the {@code user_id} FK chain — no direct FK from
 * {@code seller_profiles} to {@code user_profiles} is needed or used.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileUpdateRequest {

    // ─── Personal Info (written to UserProfile on update) ───────
    // These are stored in user_profiles table, accessed via:
    // seller_profiles.user_id → users.id → user_profiles.user_id

    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Size(max = 500, message = "Profile image URL cannot exceed 500 characters")
    private String profileImageUrl;

    @Size(max = 20, message = "Gender cannot exceed 20 characters")
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Preferred language cannot exceed 20 characters")
    private String preferredLanguage;

    @Pattern(regexp = "^(\\+?[0-9]{7,15})?$", message = "Invalid phone number format")
    private String phone;

    // ─── Basic Seller Profile ────────────────────────────────────

    @Size(min = 2, max = 200, message = "Shop name must be between 2 and 200 characters")
    private String shopName;

    @Size(min = 2, max = 200, message = "Business name must be between 2 and 200 characters")
    private String businessName;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    @Size(max = 20, message = "Business Phone must not exceed 20 characters")
    private String businessPhone;

    private String storeName;

    // ─── KYC Updates ────────────────────────────────────────────

    @Size(max = 20, message = "PAN number must not exceed 20 characters")
    private String panNumber;

    @Size(max = 150, message = "PAN name must not exceed 150 characters")
    private String panName;

    @Pattern(regexp = "^([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})?$", message = "Invalid GSTIN format")
    private String gstin;

    private Boolean gstRegistered;

    private KycBusinessType kycBusinessType;

    // ─── Document Updates ───────────────────────────────────────

    private String aadhar;

    private String registrationProof;

    // ─── Bank Updates ───────────────────────────────────────────

    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;

    // ─── Farmer Details Updates ─────────────────────────────────

    private Boolean isOwnProduce;
    private String farmLocationVillage;
    private String landArea;
    private String cropTypes;

    // ─── Business Details Updates ───────────────────────────────

    private String legalBusinessName;
    private String authorizedSignatory;
    private String warehouseLocation;

    // ─── Wholesale Config Updates ───────────────────────────────

    private Boolean bulkPricingEnabled;
    private Integer minOrderQuantity;

    // ─── Address Details Updates ───────────────────────────────
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String district;
    private String state;
    private String pincode;
    private String country;
    // Store / Warehouse Address
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
