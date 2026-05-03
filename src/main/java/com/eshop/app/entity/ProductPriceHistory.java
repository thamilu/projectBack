package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ProductPriceHistory entity for tracking price changes.
 */
@Entity
@Table(
    name = "product_price_history",
    indexes = {
        @Index(name = "idx_price_history_product", columnList = "product_id"),
        @Index(name = "idx_price_history_changed_at", columnList = "changed_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductPriceHistory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "old_price", precision = 12, scale = 2)
    private BigDecimal oldPrice;
    
    @Column(name = "new_price", precision = 12, scale = 2)
    private BigDecimal newPrice;
    
    @Column(name = "old_discount_price", precision = 12, scale = 2)
    private BigDecimal oldDiscountPrice;
    
    @Column(name = "new_discount_price", precision = 12, scale = 2)
    private BigDecimal newDiscountPrice;
    
    @Column(name = "changed_by", length = 100)
    private String changedBy;
    
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;
    
    @Column(name = "change_reason", length = 500)
    private String changeReason;
}
