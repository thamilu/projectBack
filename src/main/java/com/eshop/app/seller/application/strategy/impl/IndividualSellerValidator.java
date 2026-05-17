package com.eshop.app.seller.application.strategy.impl;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.strategy.SellerRegistrationValidator;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.core.exception.business.ValidationException;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * [HARDEN] Validation strategy for INDIVIDUAL identity type sellers.
 * Requires a valid PAN number matching the standard format.
 */
@Component
public class IndividualSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (isBlank(request.getPanNumber())) {
            throw new ValidationException("PAN number is required for INDIVIDUAL sellers", "MISSING_PAN_NUMBER");
        }

        String pan = request.getPanNumber();
        if (!pan.trim().matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$")) {
            throw new ValidationException("Invalid PAN number format", "INVALID_PAN_FORMAT");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return SellerIdentityType.INDIVIDUAL;
    }
}


