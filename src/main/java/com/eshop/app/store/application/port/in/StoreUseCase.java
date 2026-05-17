package com.eshop.app.store.application.port.in;

import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.store.api.request.StoreCreateRequest;
import com.eshop.app.store.api.response.StoreResponse;
import org.springframework.data.domain.Pageable;

/**
 * Inbound Port for Store Use Cases.
 */
public interface StoreUseCase {
    StoreResponse createStore(StoreCreateRequest request);
    StoreResponse updateStore(Long id, StoreCreateRequest request);
    void deleteStore(Long id);
    StoreResponse getStoreById(Long id);
    StoreResponse getMyStore();
    PageResponse<StoreResponse> getAllStores(Pageable pageable);
    PageResponse<StoreResponse> searchStores(String keyword, Pageable pageable);
    
    // Dashboard Analytics Methods
    long getTotalStoreCount();
    String getStoreNameBySellerId(Long sellerId);
    Double getStoreRatingBySellerId(Long sellerId);
}

