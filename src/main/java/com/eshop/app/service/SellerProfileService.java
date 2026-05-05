package com.eshop.app.service;

import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import org.springframework.security.core.Authentication;

/**
 * Enterprise-grade service for Seller Profile management.
 * Handles core profile retrieval and updates.
 */
public interface SellerProfileService {
    SellerProfileResponse getSellerProfile(Long userId);
    SellerProfileResponse getSellerProfile(Authentication authentication);
    SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request);
    boolean hasProfile(Long userId);
    boolean hasProfile(Authentication authentication);
    boolean existsByShopHandle(String handle);
}
