package com.eshop.app.seller.application.port.in;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;

/**
 * Use case for registering a new seller.
 */
public interface RegisterSellerUseCase {
    SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request);
}
