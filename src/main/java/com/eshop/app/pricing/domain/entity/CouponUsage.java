package com.eshop.app.pricing.domain.entity;

import com.eshop.app.core.kernel.BaseEntity;
import com.eshop.app.order.domain.entity.Order;
import com.eshop.app.user.domain.entity.User;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a single application of a {@link Coupon} by a user, enabling enforcement of
 * {@code usageLimitPerUser} and {@code firstTimeOnly} — {@link Coupon#canBeUsedByUser} already
 * implements that logic, but had nothing to read the per-user usage count from until this table
 * existed.
 */
@Entity
@Table(name = "coupon_usages", indexes = {
        @Index(name = "idx_coupon_usage_coupon_user", columnList = "coupon_id, user_id"),
        @Index(name = "idx_coupon_usage_user_id", columnList = "user_id"),
        @Index(name = "idx_coupon_usage_order_id", columnList = "order_id")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;
}
