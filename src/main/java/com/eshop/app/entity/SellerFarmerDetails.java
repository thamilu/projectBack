package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Farmer-specific details for sellers with identity_type = FARMER.
 * Only populated when the seller is a farmer — keeps SellerProfile clean.
 */
@Entity
@Table(name = "seller_farmer_details", indexes = {
        @Index(name = "idx_farmer_details_profile", columnList = "seller_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerFarmerDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, unique = true)
    private SellerProfile sellerProfile;

    @Column(name = "is_own_produce")
    @Builder.Default
    private Boolean isOwnProduce = false;

    @Column(name = "farm_location", length = 250)
    private String farmLocation;

    @Column(name = "land_area", length = 50)
    private String landArea;

    @Column(name = "crop_types", columnDefinition = "TEXT")
    private String cropTypes;
}
