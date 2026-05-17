package com.eshop.app.seller.application.strategy.impl;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.strategy.SellerRegistrationValidator;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.core.exception.business.ValidationException;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * [HARDEN] Global validation strategy for ALL seller types.
 * Enforces mandatory fields regardless of identity type:
 * - Shop name
 * - Residential district
 * - Store/warehouse physical address and pincode
 *
 * getSupportedType() returns null â€” this is a global validator applied to every registration.
 */
@Component
public class GlobalSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (isBlank(request.getShopName())) {
            throw new ValidationException("Shop name is mandatory", "MISSING_SHOP_NAME");
        }

        if (isBlank(request.getDistrict())) {
            throw new ValidationException("Residential district is required", "MISSING_DISTRICT");
        }

        if (isBlank(request.getStoreAddressLine1())
                || isBlank(request.getStoreCity())
                || isBlank(request.getStoreDistrict())) {
            throw new ValidationException(
                "Store/Warehouse address including district is required", "MISSING_STORE_ADDRESS");
        }

        if (isBlank(request.getStorePincode()) || !request.getStorePincode().matches("^[0-9]{6}$")) {
            throw new ValidationException("Store Pincode must be exactly 6 digits", "INVALID_STORE_PINCODE");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return null; // Global validator â€” applied to all registrations
    }
}


