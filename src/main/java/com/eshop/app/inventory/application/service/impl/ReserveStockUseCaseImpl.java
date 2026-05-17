package com.eshop.app.inventory.application.service.impl;

import com.eshop.app.inventory.application.port.in.ReserveStockUseCase;
import com.eshop.app.inventory.domain.entity.Inventory;
import com.eshop.app.inventory.domain.repository.InventoryRepository;
import com.eshop.app.core.exception.business.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReserveStockUseCaseImpl implements ReserveStockUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public boolean reserveStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        boolean success = inventory.reserveStock(quantity);
        if (success) {
            inventoryRepository.save(inventory);
        }
        return success;
    }

    @Override
    public void releaseReservedStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        inventory.releaseReservedStock(quantity);
        inventoryRepository.save(inventory);
    }
}


