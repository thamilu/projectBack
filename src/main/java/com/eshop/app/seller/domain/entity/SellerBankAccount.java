package com.eshop.app.seller.domain.entity;

import com.eshop.app.seller.shared.domain.enums.KycVerificationStatus;
import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.user.domain.entity.SellerProfile;

import jakarta.persistence.*;
import lombok.*;

/**
 * Seller bank account for payouts.
 * Supports multiple accounts with one primary account for settlements.
 */
@Entity
@Table(name = "seller_bank_accounts", indexes = {
        @Index(name = "idx_bank_account_profile", columnList = "seller_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerBankAccount extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;

    @Column(name = "account_holder_name", length = 150)
    private String accountHolderName;

    @Column(name = "account_number", length = 255)
    @jakarta.persistence.Convert(converter = com.eshop.app.catalog.infrastructure.security.AttributeEncryptor.class)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "bank_name", length = 200)
    private String bankName;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private KycVerificationStatus verificationStatus = KycVerificationStatus.PENDING;
}

