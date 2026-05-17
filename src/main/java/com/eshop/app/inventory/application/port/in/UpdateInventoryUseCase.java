package com.eshop.app.inventory.application.port.in;

/**
 * Use case for updating inventory levels.
 */
public interface UpdateInventoryUseCase {
    void addStock(Long productId, int quantity);
    void setStock(Long productId, int quantity);
    void confirmSale(Long productId, int quantity);
}
