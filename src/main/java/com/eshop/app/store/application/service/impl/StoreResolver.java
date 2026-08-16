package com.eshop.app.store.application.service.impl;

import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.store.api.response.StoreResponse;
import com.eshop.app.store.application.port.in.StoreUseCase;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Enterprise service component to resolve stores for seller operations. Centralises the store
 * fetching and validation logic to satisfy the DRY principle.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StoreResolver {

    private final StoreUseCase storeService;
    private final StoreRepository storeRepository;

    /**
     * Resolves the current authenticated seller's store.
     *
     * @return the resolved Store entity
     * @throws ResourceNotFoundException if the seller has no store or the store doesn't exist
     */
    public Store resolveCurrentSellerStore() {
        StoreResponse myStore = storeService.getMyStore();
        if (myStore == null || myStore.getId() == null) {
            log.warn("Authenticated seller has no associated store");
            throw new ResourceNotFoundException(
                    "Seller must create a store before this operation.");
        }
        return storeRepository
                .findById(myStore.getId())
                .orElseThrow(
                        () -> {
                            log.error("Store with id={} not found in database", myStore.getId());
                            return new ResourceNotFoundException("Store not found");
                        });
    }

    /**
     * Resolves a store by ID, falling back to the current authenticated seller's store if null.
     *
     * @param storeId the optional store ID
     * @return the resolved Store entity
     */
    public Store resolveStore(Long storeId) {
        if (storeId == null) {
            return resolveCurrentSellerStore();
        }
        return storeRepository
                .findById(storeId)
                .orElseThrow(
                        () -> {
                            log.error("Store with id={} not found in database", storeId);
                            return new ResourceNotFoundException(
                                    "Store not found with id: " + storeId);
                        });
    }
}
