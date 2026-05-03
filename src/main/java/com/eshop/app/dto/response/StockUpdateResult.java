package com.eshop.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of a stock update operation.
 * Provides complete audit trail of stock changes.
 * 
 * @since 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockUpdateResult {
    
    private Long productId;
    private Integer previousStock;
    private Integer newStock;
    private Integer delta;
    private LocalDateTime updatedAt;
    private String reason;
    
    /**
     * Factory method for creating update results
     */
    public static StockUpdateResult of(Long productId, int previousStock, int newStock, int delta) {
        return StockUpdateResult.builder()
                .productId(productId)
                .previousStock(previousStock)
                .newStock(newStock)
                .delta(delta)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
