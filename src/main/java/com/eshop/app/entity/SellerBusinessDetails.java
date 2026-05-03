package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Business-specific details for sellers with identity_type = BUSINESS.
 * Stores legal entity information, signatory, and warehouse location.
 */
@Entity
@Table(name = "seller_business_details", indexes = {
        @Index(name = "idx_business_details_profile", columnList = "seller_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerBusinessDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, unique = true)
    private SellerProfile sellerProfile;

    @Column(name = "legal_business_name", length = 200)
    private String legalBusinessName;

    @Column(name = "authorized_signatory", length = 150)
    private String authorizedSignatory;

    @Column(name = "warehouse_location", length = 255)
    private String warehouseLocation;
}
