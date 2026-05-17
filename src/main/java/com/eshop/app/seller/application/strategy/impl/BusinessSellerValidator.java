package com.eshop.app.seller.application.strategy.impl;

import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.strategy.SellerRegistrationValidator;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.core.exception.business.ValidationException;
import org.springframework.stereotype.Component;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * [HARDEN] Validation strategy for BUSINESS identity type sellers.
 * Requires GSTIN or PAN, validates GSTIN format, and enforces legal business name.
 *
 * Moved from root strategy/impl/ to seller/application/strategy/impl/ â€” proper bounded context.
 */
@Component
public class BusinessSellerValidator implements SellerRegistrationValidator {

    @Override
    public void validate(SellerRegisterRequest request) {
        if (isBlank(request.getGstin()) && isBlank(request.getPanNumber())) {
            throw new ValidationException("GSTIN or PAN is required for BUSINESS sellers", "MISSING_TAX_IDENTIFIER");
        }

        if (!isBlank(request.getGstin())
                && !request.getGstin().matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$")) {
            throw new ValidationException("Invalid GSTIN format", "INVALID_GSTIN_FORMAT");
        }

        if (isBlank(request.getLegalBusinessName())) {
            throw new ValidationException("Legal Business Name is required for BUSINESS sellers", "MISSING_LEGAL_NAME");
        }
    }

    @Override
    public SellerIdentityType getSupportedType() {
        return SellerIdentityType.BUSINESS;
    }
}


