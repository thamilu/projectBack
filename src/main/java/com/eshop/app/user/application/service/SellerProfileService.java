package com.eshop.app.user.application.service;

import com.eshop.app.user.api.request.SellerProfileUpdateRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;




import org.springframework.security.core.Authentication;

/**
 * Enterprise-grade service for Seller Profile management.
 * Handles core profile retrieval and updates.
 */
public interface SellerProfileService extends com.eshop.app.user.application.port.in.SellerProfileUseCase {
    SellerProfileResponse getSellerProfile(Long userId);
    SellerProfileResponse getSellerProfile(Authentication authentication);
    SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request);
    boolean hasProfile(Long userId);
    boolean hasProfile(Authentication authentication);
    boolean existsByShopHandle(String handle);
}
