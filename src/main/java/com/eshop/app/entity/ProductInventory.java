package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ProductInventory entity for multi-warehouse support.
 */
@Entity
@Table(
    name = "product_inventory",
    indexes = {
        @Index(name = "idx_inventory_product", columnList = "product_id"),
        @Index(name = "idx_inventory_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_inventory_product_warehouse", columnList = "product_id, warehouse_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_warehouse", columnNames = {"product_id", "warehouse_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;
    
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 0;
    
    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;
    
    @Column(name = "reorder_level")
    private Integer reorderLevel;
    
    /**
     * Gets the available quantity (total - reserved).
     */
    public int getAvailableQuantity() {
        int total = quantity != null ? quantity : 0;
        int reserved = reservedQuantity != null ? reservedQuantity : 0;
        return Math.max(0, total - reserved);
    }
}
