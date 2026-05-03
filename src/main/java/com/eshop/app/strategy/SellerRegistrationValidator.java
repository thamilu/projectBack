package com.eshop.app.strategy;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.enums.SellerIdentityType;

/**
 * Strategy interface for identity-specific seller registration validation.
 * 
 * <p>Implementation of this interface should handle validation specific to a 
 * particular {@link SellerIdentityType}.
 */
public interface SellerRegistrationValidator {
    
    /**
     * Validates the registration request for a specific identity type.
     *
     * @param request the registration request to validate
     * @throws com.eshop.app.exception.ValidationException if validation fails
     */
    void validate(SellerRegisterRequest request);

    /**
     * Returns the identity type this validator supports.
     *
     * @return the supported {@link SellerIdentityType}
     */
    SellerIdentityType getSupportedType();
}
