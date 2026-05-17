package com.eshop.app.seller.application.strategy;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.core.exception.business.ValidationException;

/**
 * [HARDEN] Strategy interface for identity-specific seller registration validation.
 *
 * Moved from root strategy/ to seller/application/strategy/ â€” proper bounded context ownership.
 * Each implementation handles validation for one specific SellerIdentityType.
 *
 * @see SellerIdentityType
 */
public interface SellerRegistrationValidator {

    /**
     * Validates the registration request for this specific identity type.
     *
     * @param request the registration request to validate
     * @throws ValidationException if validation fails
     */
    void validate(SellerRegisterRequest request);

    /**
     * Returns the identity type this validator supports.
     * Used for automatic strategy routing.
     *
     * @return the supported SellerIdentityType
     */
    SellerIdentityType getSupportedType();
}


