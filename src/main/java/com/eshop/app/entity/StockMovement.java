package com.eshop.app.entity;

import com.eshop.app.core.events.domain.StockChangedEvent;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Audit entity for tracking product stock movements.
 */
@Entity
@Table(name = "stock_movements", indexes = {
    @Index(name = "idx_stock_mov_product", columnList = "product_id"),
    @Index(name = "idx_stock_mov_created", columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "previous_stock", nullable = false)
    private Integer previousStock;

    @Column(name = "new_stock", nullable = false)
    private Integer newStock;

    @Column(name = "delta", nullable = false)
    private Integer delta;

    @Column(name = "reason", length = 255)
    private String reason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Conversion constructor from domain event.
     */
    public StockMovement(StockChangedEvent event, Product product) {
        this.product = product;
        this.previousStock = event.getPreviousStock();
        this.newStock = event.getNewStock();
        this.delta = event.getDelta();
        this.reason = event.getReason();
    }
}
