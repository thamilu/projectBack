package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tax_rules", indexes = {
    @Index(name = "idx_tax_rule_class", columnList = "tax_class_id"),
    @Index(name = "idx_tax_rule_rate", columnList = "tax_rate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxRule extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id", nullable = false)
    private TaxClass taxClass;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_rate_id", nullable = false)
    private TaxRate taxRate;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}
