package com.eshop.app.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inventory entity for stock management and tracking
 * Provides real-time inventory levels with transaction history
 * 
 * Time Complexity: O(1) for stock operations with proper indexing
 * Space Complexity: O(1) per product inventory record
 */
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "product_id", unique = true),
        @Index(name = "idx_inventory_store_id", columnList = "store_id"),
    @Index(name = "idx_inventory_low_stock", columnList = "quantity,low_stock_threshold"),
    @Index(name = "idx_inventory_out_of_stock", columnList = "quantity"),
    @Index(name = "idx_inventory_updated_at", columnList = "updated_at")
})
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory extends BaseEntity {
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @Column(name = "quantity", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer quantity = 0;
    
    @Column(name = "reserved_quantity", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer reservedQuantity = 0; // Items in pending orders
    
    @Column(name = "available_quantity", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer availableQuantity = 0; // quantity - reservedQuantity
    
    @Column(name = "low_stock_threshold", nullable = false, columnDefinition = "INTEGER DEFAULT 10")
    @Builder.Default
    private Integer lowStockThreshold = 10;
    
    @Column(name = "reorder_point", columnDefinition = "INTEGER DEFAULT 5")
    @Builder.Default
    private Integer reorderPoint = 5;
    
    @Column(name = "reorder_quantity", columnDefinition = "INTEGER DEFAULT 50")
    @Builder.Default
    private Integer reorderQuantity = 50;
    
    @Column(name = "last_restocked_at")
    private LocalDateTime lastRestockedAt;
    
    @Column(name = "last_sold_at")
    private LocalDateTime lastSoldAt;
    
    @Column(name = "total_sold", columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer totalSold = 0;
    
    @Column(name = "total_received", columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private Integer totalReceived = 0;
    
    @Column(name = "is_tracking_enabled", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Builder.Default
    private Boolean isTrackingEnabled = true;
    
    @Column(name = "supplier_id")
    private Long supplierId; // Reference to supplier (could be separate entity)
    
    @Column(name = "supplier_sku", length = 100)
    private String supplierSku;
    
    @Column(name = "location", length = 100)
    private String location; // Warehouse location
    
    /**
     * Check if product is in stock
     */
    public boolean isInStock() {
        return availableQuantity > 0;
    }
    
    /**
     * Check if stock is low
     */
    public boolean isLowStock() {
        return availableQuantity <= lowStockThreshold && availableQuantity > 0;
    }
    
    /**
     * Check if needs reorder
     */
    public boolean needsReorder() {
        return availableQuantity <= reorderPoint;
    }
    
    /**
     * Reserve stock for order
     */
    public boolean reserveStock(int quantityToReserve) {
        if (availableQuantity >= quantityToReserve) {
            this.reservedQuantity += quantityToReserve;
            this.availableQuantity = this.quantity - this.reservedQuantity;
            return true;
        }
        return false;
    }
    
    /**
     * Release reserved stock (order cancelled)
     */
    public void releaseReservedStock(int quantityToRelease) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantityToRelease);
        this.availableQuantity = this.quantity - this.reservedQuantity;
    }
    
    /**
     * Confirm sale (reduce actual stock)
     */
    public void confirmSale(int quantitySold) {
        this.quantity = Math.max(0, this.quantity - quantitySold);
        this.reservedQuantity = Math.max(0, this.reservedQuantity - quantitySold);
        this.availableQuantity = this.quantity - this.reservedQuantity;
        this.totalSold += quantitySold;
        this.lastSoldAt = LocalDateTime.now();
    }
    
    /**
     * Add stock (restock)
     */
    public void addStock(int quantityToAdd) {
        this.quantity += quantityToAdd;
        this.availableQuantity = this.quantity - this.reservedQuantity;
        this.totalReceived += quantityToAdd;
        this.lastRestockedAt = LocalDateTime.now();
    }
    
    /**
     * Set stock level directly
     */
    public void setStock(int newQuantity) {
        this.quantity = Math.max(0, newQuantity);
        this.availableQuantity = this.quantity - this.reservedQuantity;
    }
    
    /**
     * Get stock status
     */
    public StockStatus getStockStatus() {
        if (availableQuantity == 0) {
            return StockStatus.OUT_OF_STOCK;
        } else if (isLowStock()) {
            return StockStatus.LOW_STOCK;
        } else if (needsReorder()) {
            return StockStatus.REORDER_NEEDED;
        } else {
            return StockStatus.IN_STOCK;
        }
    }
    
    public enum StockStatus {
        IN_STOCK,
        LOW_STOCK,
        OUT_OF_STOCK,
        REORDER_NEEDED
    }
}
