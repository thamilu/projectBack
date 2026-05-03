package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "product_to_store", indexes = {
    @Index(name = "idx_product_store_product", columnList = "product_id"),
    @Index(name = "idx_product_store_store", columnList = "store_id"),
    @Index(name = "idx_product_store_unique", columnList = "product_id,store_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductToStore extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "store_specific_price", precision = 10, scale = 2)
    private BigDecimal storeSpecificPrice;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean visible = true;
    
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;
}
