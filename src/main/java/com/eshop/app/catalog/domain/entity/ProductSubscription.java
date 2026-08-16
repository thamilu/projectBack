package com.eshop.app.catalog.domain.entity;

import com.eshop.app.subscription.domain.entity.SubscriptionInterval;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/** ProductSubscription entity representing subscription details. */
@Entity
@Table(name = "product_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSubscription {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(
            name = "product_id",
            foreignKey = @ForeignKey(name = "fk_product_subscription_product"))
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_interval", length = 20)
    private SubscriptionInterval subscriptionInterval;

    @Min(value = 1)
    @Column(name = "subscription_interval_count")
    private Integer subscriptionIntervalCount;

    @Min(value = 0)
    @Column(name = "trial_days")
    private Integer trialDays;
}
