package com.eshop.app.strategy.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.enums.SellerBusinessType;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.strategy.SellerRegistrationValidator;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Validator for FARMER business activity.
 * Ensures that if a seller identifies as a farmer, they provide farm details.
 * 
 * Note: This returns null for getSupportedType() as it is an activity validator, 
 * not an identity-type validator. We will handle these specially in SellerService.
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
        // null indicates this is a generic activity validator, not bound to a single IdentityType
        return null;
    }
}
