package com.eshop.app.seller.application.processor.impl;



import com.eshop.app.seller.application.processor.SellerModuleProcessor;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.domain.entity.SellerBusinessDetails;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.user.domain.entity.SellerProfile;
import org.springframework.stereotype.Component;

/**
 * [HARDEN] Business details registration module processor.
 * Applicable only when seller identity type is BUSINESS.
 * Execution order: 40 (after KYC and Farmer processors).
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
