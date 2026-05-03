package com.eshop.app.processor.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.entity.SellerBusinessDetails;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.processor.SellerModuleProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor for Business-specific details.
 * Applicable if the seller identity type is BUSINESS.
 */
@Component
public class BusinessDetailsProcessor implements SellerModuleProcessor {

    @Override
    public void process(SellerProfile profile, SellerRegisterRequest request) {
        if (request.getLegalBusinessName() == null && request.getAuthorizedSignatory() == null
                && request.getWarehouseLocation() == null) return;

        SellerBusinessDetails d = profile.getBusinessDetails() != null
                ? profile.getBusinessDetails() : new SellerBusinessDetails();
        
        d.setSellerProfile(profile);
        d.setLegalBusinessName(request.getLegalBusinessName());
        d.setAuthorizedSignatory(request.getAuthorizedSignatory());
        d.setWarehouseLocation(request.getWarehouseLocation());
        
        profile.setBusinessDetails(d);
    }

    @Override
    public boolean isApplicable(SellerRegisterRequest request) {
        return request.getIdentityType() == SellerIdentityType.BUSINESS;
    }

    @Override
    public int getOrder() {
        return 40;
    }
}
