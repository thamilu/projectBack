package com.eshop.app.strategy.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.strategy.SellerRegistrationValidator;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Validation strategy for BUSINESS identity type.
 * Requires either GSTIN or PAN for registered entities.
 */
@Component
public class BusinessSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (isBlank(request.getGstin()) && isBlank(request.getPanNumber())) {
            throw new ValidationException("GSTIN or PAN is required for BUSINESS sellers", "MISSING_TAX_IDENTIFIER");
        }
        
        if (!isBlank(request.getGstin()) && !request.getGstin().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
            throw new ValidationException("Invalid GSTIN format", "INVALID_GSTIN_FORMAT");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return SellerIdentityType.BUSINESS;
    }
}
