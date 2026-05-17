package com.eshop.app.inventory.application.port.in;

/**
 * Use case for reserving and releasing stock.
 */
public interface ReserveStockUseCase {
    boolean reserveStock(Long productId, int quantity);
    void releaseReservedStock(Long productId, int quantity);
}
