package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "subscription_plans", indexes = {
    @Index(name = "idx_subscription_plan_product", columnList = "product_id"),
    @Index(name = "idx_subscription_plan_active", columnList = "active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPlan extends BaseEntity {
    
    @Column(nullable = false, length = 200)
    private String name;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BillingCycle billingCycle;
    
    @Column(name = "cycle_length", nullable = false)
    @Builder.Default
    private Integer cycleLength = 1;
    
    @Column(name = "trial_days")
    @Builder.Default
    private Integer trialDays = 0;
    
    @Column(name = "trial_price", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal trialPrice = BigDecimal.ZERO;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    public enum BillingCycle {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
    }
}
