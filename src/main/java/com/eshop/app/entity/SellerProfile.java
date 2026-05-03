package com.eshop.app.entity;

import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerBusinessType;
import com.eshop.app.enums.SellerStatus;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.persistence.*;
import lombok.*;

/**
 * Core seller profile — minimal and clean.
 * <p>
 * Contains only identity, lifecycle, and display information.
 * KYC, documents, bank accounts, farmer/business details are in separate tables.
 * </p>
 *
 * <pre>
 * Architecture:
 * User → SellerProfile (direct FK via shared user_id)
 *   ├── SellerKYC
 *   ├── SellerDocument
 *   ├── SellerBankAccount
 *   ├── SellerBusinessDetails (optional)
 *   ├── SellerFarmerDetails (optional)
 *   ├── SellerWholesaleConfig (optional)
 *   └── Store
 * </pre>
 */
@Entity
@Table(name = "seller_profiles", indexes = {
        @Index(name = "idx_seller_profile_user_id", columnList = "user_id"),
        @Index(name = "idx_seller_profile_status", columnList = "status"),
        @Index(name = "idx_seller_profile_identity_type", columnList = "identity_type"),
        @Index(name = "idx_seller_profile_shop_name", columnList = "shop_name")
})
@NamedEntityGraph(
    name = "SellerProfile.full",
    attributeNodes = {
        @NamedAttributeNode("user"),
        @NamedAttributeNode("kyc"),
        @NamedAttributeNode("farmerDetails"),
        @NamedAttributeNode("businessDetails"),
        @NamedAttributeNode("wholesaleConfig"),
        @NamedAttributeNode("bankAccounts"),
        @NamedAttributeNode("documents"),
        @NamedAttributeNode("stores")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "user", "bankAccounts", "documents", "stores" })
@EqualsAndHashCode(callSuper = true, exclude = { "user", "bankAccounts", "documents", "stores" })
public class SellerProfile extends BaseEntity {

    // ─── Identity ───────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 20)
    private SellerIdentityType identityType;

    @Column(name = "shop_name", nullable = false, length = 200)
    private String shopName;

    @ElementCollection(targetClass = SellerBusinessType.class)
    @CollectionTable(name = "seller_business_types", joinColumns = @JoinColumn(name = "seller_profile_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "business_type")
    private Set<SellerBusinessType> businessTypes;

    @Column(name = "business_name", length = 200)
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String businessMobileNumber;

    // ─── Address & Location ─────────────────────────────────────

    @Column(name = "address_line_1", length = 500)
    private String addressLine1;

    @Column(name = "address_line_2", length = 500)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(length = 100)
    private String state;

    @Column(length = 20)
    private String pincode;

    @Column(length = 100)
    private String country;

    // ─── Store / Warehouse Location ─────────────────────────────

    @Column(name = "store_address_line1", length = 500)
    private String storeAddressLine1;

    @Column(name = "store_address_line2", length = 500)
    private String storeAddressLine2;

    @Column(name = "store_city", length = 100)
    private String storeCity;

    @Column(name = "store_district", length = 100)
    private String storeDistrict;

    @Column(name = "store_state", length = 100)
    private String storeState;

    @Column(name = "store_pincode", length = 20)
    private String storePincode;

    @Column(name = "store_country", length = 100)
    private String storeCountry;

    @Column(name = "google_maps_url", length = 500)
    private String googleMapsUrl;

    // ─── Lifecycle ──────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SellerStatus status = SellerStatus.PENDING;

    // ─── Relationships ──────────────────────────────────────────

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;



    @OneToOne(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerKYC kyc;

    @OneToOne(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerFarmerDetails farmerDetails;

    @OneToOne(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerBusinessDetails businessDetails;

    @OneToOne(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private SellerWholesaleConfig wholesaleConfig;

    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SellerBankAccount> bankAccounts = new HashSet<>();

    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<SellerDocument> documents = new HashSet<>();

    @OneToMany(mappedBy = "sellerProfile", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Store> stores = new HashSet<>();

    // ─── Audit ──────────────────────────────────────────────────

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;
}
