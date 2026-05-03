package com.eshop.app.entity;

import com.eshop.app.enums.KycBusinessType;
import com.eshop.app.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * KYC (Know Your Customer) details for seller compliance.
 * Stores PAN, GSTIN, and verification workflow — isolated from SellerProfile
 * for security and clean separation of concerns.
 */
@Entity
@Table(name = "seller_kyc", indexes = {
        @Index(name = "idx_seller_kyc_profile", columnList = "seller_profile_id"),
        @Index(name = "idx_seller_kyc_pan", columnList = "pan_number"),
        @Index(name = "idx_seller_kyc_gstin", columnList = "gstin")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerKYC extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, unique = true)
    private SellerProfile sellerProfile;

    @Column(name = "pan_number", length = 20, nullable = false)
    private String panNumber;

    @Column(name = "pan_name", length = 150)
    private String panName;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @Column(name = "gst_registered")
    @Builder.Default
    private Boolean gstRegistered = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "business_type", length = 30)
    private KycBusinessType businessType;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private KycVerificationStatus verificationStatus = KycVerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;
}
