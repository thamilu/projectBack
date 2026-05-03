package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_attributes", indexes = {
    @Index(name = "idx_attr_product", columnList = "product_id"),
    @Index(name = "idx_attr_name", columnList = "attribute_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductAttribute extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @Column(name = "attribute_name", nullable = false, length = 100)
    private String attributeName;
    
    @Column(name = "attribute_value", length = 500)
    private String attributeValue;
    
    @Column(name = "display_order")
    private Integer displayOrder;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
}