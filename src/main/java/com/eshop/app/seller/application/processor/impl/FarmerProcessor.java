package com.eshop.app.seller.application.processor.impl;



import com.eshop.app.seller.application.processor.SellerModuleProcessor;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.domain.entity.SellerFarmerDetails;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.user.domain.entity.SellerProfile;
import org.springframework.stereotype.Component;

/**
 * [HARDEN] Farmer details registration module processor.
 * Applicable when seller has FARMER in their businessTypes.
 * Execution order: 30.
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
        return request.getBusinessTypes() != null
            && request.getBusinessTypes().contains(SellerBusinessType.FARMER);
    }

    @Override
    public int getOrder() {
        return 30;
    }
}
