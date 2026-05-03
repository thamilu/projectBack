package com.eshop.app.entity;

import com.eshop.app.enums.DocumentType;
import com.eshop.app.enums.KycVerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Seller document storage — files, document numbers, and verification workflow.
 * Supports multiple documents per seller (PAN, Aadhaar, GST Certificate, License, etc.).
 */
@Entity
@Table(name = "seller_documents", indexes = {
        @Index(name = "idx_seller_doc_profile", columnList = "seller_profile_id"),
        @Index(name = "idx_seller_doc_type", columnList = "document_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false)
    private SellerProfile sellerProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Column(name = "document_url", length = 500)
    private String documentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false, length = 20)
    @Builder.Default
    private KycVerificationStatus verificationStatus = KycVerificationStatus.PENDING;

    /**
     * @deprecated Use {@link #verificationStatus} instead. Kept for backward compatibility.
     */
    @Deprecated
    @Transient
    public Boolean getVerified() {
        return verificationStatus == KycVerificationStatus.VERIFIED;
    }

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
