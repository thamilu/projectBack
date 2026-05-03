package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "category_commissions", indexes = {
    @Index(name = "idx_comm_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCommission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "commission_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionPercentage; // e.g., 5.00 for 5%

    @Column(name = "flat_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal flatFee = BigDecimal.ZERO;

    @Column(name = "gst_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal gstPercentage = new BigDecimal("18.00");

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
