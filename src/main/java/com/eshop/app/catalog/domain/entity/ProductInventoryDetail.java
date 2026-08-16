package com.eshop.app.catalog.domain.entity;

import com.eshop.app.inventory.domain.entity.StockStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

/** ProductInventoryDetail entity representing single-warehouse stock configuration. */
@Entity
@Table(name = "product_inventory_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductInventoryDetail {

    @Id
    @Column(name = "product_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(
            name = "product_id",
            foreignKey = @ForeignKey(name = "fk_product_inv_detail_product"))
    private Product product;

    @Min(value = 0, message = "Stock quantity cannot be negative")
    @Column(name = "stock_quantity")
    @Builder.Default
    private Integer stockQuantity = 0;

    @Min(value = 0)
    @Column(name = "reserved_quantity")
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Min(value = 0)
    @Column(name = "reorder_level")
    private Integer reorderLevel;

    @Min(value = 1)
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;

    @Column(name = "track_inventory", nullable = false)
    @Builder.Default
    private boolean trackInventory = true;

    @Column(name = "allow_backorder", nullable = false)
    @Builder.Default
    private boolean allowBackorder = false;

    @Min(value = 1)
    @Column(name = "backorder_lead_days")
    private Integer backorderLeadDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 30)
    @Builder.Default
    private StockStatus stockStatus = StockStatus.IN_STOCK;
}
