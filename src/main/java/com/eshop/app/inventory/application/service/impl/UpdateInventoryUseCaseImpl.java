package com.eshop.app.inventory.application.service.impl;

import com.eshop.app.inventory.application.port.in.UpdateInventoryUseCase;
import com.eshop.app.inventory.domain.entity.Inventory;
import com.eshop.app.inventory.domain.repository.InventoryRepository;
import com.eshop.app.core.exception.business.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UpdateInventoryUseCaseImpl implements UpdateInventoryUseCase {

    private final InventoryRepository inventoryRepository;

    @Override
    public void addStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        inventory.addStock(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void setStock(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        inventory.setStock(quantity);
        inventoryRepository.save(inventory);
    }

    @Override
    public void confirmSale(Long productId, int quantity) {
        Inventory inventory = inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product: " + productId));
        inventory.confirmSale(quantity);
        inventoryRepository.save(inventory);
    }
}


