package com.eshop.app.strategy.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.strategy.SellerRegistrationValidator;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Validation strategy for INDIVIDUAL identity type.
 * Requires PAN number for individual sellers.
 */
@Component
public class IndividualSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (isBlank(request.getPanNumber())) {
            throw new ValidationException("PAN number is required for INDIVIDUAL sellers", "MISSING_PAN_NUMBER");
        }

        if (!request.getPanNumber().matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
            throw new ValidationException("Invalid PAN number format", "INVALID_PAN_FORMAT");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return SellerIdentityType.INDIVIDUAL;
    }
}
