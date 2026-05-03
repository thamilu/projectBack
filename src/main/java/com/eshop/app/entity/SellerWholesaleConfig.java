package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Wholesale/bulk pricing configuration for sellers who offer bulk deals.
 * Optional module — only populated when bulk features are enabled.
 */
@Entity
@Table(name = "seller_wholesale_config", indexes = {
        @Index(name = "idx_wholesale_config_profile", columnList = "seller_profile_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sellerProfile")
@EqualsAndHashCode(callSuper = true, exclude = "sellerProfile")
public class SellerWholesaleConfig extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_profile_id", nullable = false, unique = true)
    private SellerProfile sellerProfile;

    @Column(name = "bulk_pricing_enabled")
    @Builder.Default
    private Boolean bulkPricingEnabled = false;

    @Column(name = "min_order_quantity")
    private Integer minOrderQuantity;
}
