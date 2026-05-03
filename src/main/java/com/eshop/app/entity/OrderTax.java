package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_taxes", indexes = {
    @Index(name = "idx_order_tax_order", columnList = "order_id"),
    @Index(name = "idx_order_tax_rate", columnList = "tax_rate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderTax extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id", nullable = false)
    private TaxRate taxRate;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
}
