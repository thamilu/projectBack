package com.eshop.app.processor.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.entity.SellerFarmerDetails;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.enums.SellerBusinessType;
import com.eshop.app.processor.SellerModuleProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor for Farmer-specific details.
 * Applicable if the seller has the FARMER business category.
 */
@Component
public class FarmerProcessor implements SellerModuleProcessor {

    @Override
    public void process(SellerProfile profile, SellerRegisterRequest request) {
        if (request.getFarmLocationVillage() == null && request.getLandArea() == null
                && request.getIsOwnProduce() == null) return;

        SellerFarmerDetails d = profile.getFarmerDetails() != null
                ? profile.getFarmerDetails() : new SellerFarmerDetails();
        
        d.setSellerProfile(profile);
        d.setFarmLocation(request.getFarmLocationVillage());
        d.setLandArea(request.getLandArea());
        d.setIsOwnProduce(request.getIsOwnProduce());
        d.setCropTypes(request.getCropTypes());
        
        profile.setFarmerDetails(d);
    }

    @Override
    public boolean isApplicable(SellerRegisterRequest request) {
        return request.getBusinessTypes() != null && 
               request.getBusinessTypes().contains(SellerBusinessType.FARMER);
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
