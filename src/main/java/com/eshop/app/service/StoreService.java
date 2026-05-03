package com.eshop.app.service;

import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.StoreResponse;
import org.springframework.data.domain.Pageable;

public interface StoreService {
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
