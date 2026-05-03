package com.eshop.app.strategy.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.strategy.SellerRegistrationValidator;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Global validation strategy for ALL seller types.
 * Enforces mandatory fields that are required regardless of identity type.
 */
@Component
public class GlobalSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        // 1. Core Shop Validation
        if (isBlank(request.getShopName())) {
            throw new ValidationException("Shop name is mandatory", "MISSING_SHOP_NAME");
        }

        // 2. Residential Address Validation
        if (isBlank(request.getDistrict())) {
            throw new ValidationException("Residential district is required", "MISSING_DISTRICT");
        }

        // 3. Store Address Validation (Physical presence check)
        if (isBlank(request.getStoreAddressLine1()) || isBlank(request.getStoreCity()) || isBlank(request.getStoreDistrict())) {
            throw new ValidationException("Store/Warehouse address including district is required", "MISSING_STORE_ADDRESS");
        }
        
        if (isBlank(request.getStorePincode()) || !request.getStorePincode().matches("^[0-9]{6}$")) {
            throw new ValidationException("Store Pincode must be exactly 6 digits", "INVALID_STORE_PINCODE");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        // Return null to indicate this is a global validator 
        // (already filtered in SellerRegistrationServiceImpl constructor)
        return null;
    }
}
