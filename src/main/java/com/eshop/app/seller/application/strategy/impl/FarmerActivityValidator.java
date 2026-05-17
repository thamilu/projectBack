package com.eshop.app.seller.application.strategy.impl;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.strategy.SellerRegistrationValidator;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.core.exception.business.ValidationException;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * [HARDEN] Activity validator for FARMER business type sellers.
 * Requires farm location and own-produce declaration when businessTypes contains FARMER.
 *
 * getSupportedType() returns null â€” this is a cross-identity activity validator,
 * not bound to a single SellerIdentityType.
 */
@Component
public class FarmerActivityValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (request.getBusinessTypes() != null && request.getBusinessTypes().contains(SellerBusinessType.FARMER)) {
            if (isBlank(request.getFarmLocationVillage())) {
                throw new ValidationException("Farm location (village) is required for FARMER sellers", "MISSING_FARM_LOCATION");
            }
            if (request.getIsOwnProduce() == null) {
                throw new ValidationException("Declaration of own produce is required for FARMER sellers", "MISSING_OWN_PRODUCE_DECLARATION");
            }
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return null; // Cross-identity activity validator
    }
}


