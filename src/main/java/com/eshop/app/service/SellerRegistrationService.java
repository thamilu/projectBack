package com.eshop.app.service;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;

/**
 * Enterprise-grade service for Seller Registration.
 * Handles validation strategies and modular registration processors.
 */
public interface SellerRegistrationService {
    SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request);
}
