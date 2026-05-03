package com.eshop.app.processor.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.entity.SellerKYC;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.processor.SellerModuleProcessor;
import org.springframework.stereotype.Component;

/**
 * Processor for Seller KYC module.
 * Handles PAN and GSTIN details.
 */
@Component
public class KycProcessor implements SellerModuleProcessor {

    @Override
    public void process(SellerProfile profile, SellerRegisterRequest request) {
        if (request.getPanNumber() == null && request.getGstin() == null) return;

        SellerKYC kyc = profile.getKyc() != null ? profile.getKyc() : new SellerKYC();
        kyc.setSellerProfile(profile);
        kyc.setPanNumber(request.getPanNumber());
        kyc.setGstin(request.getGstin());
        kyc.setGstRegistered(request.getGstin() != null && !request.getGstin().isBlank());
        
        profile.setKyc(kyc);
    }

    @Override
    public boolean isApplicable(SellerRegisterRequest request) {
        return request.getPanNumber() != null || request.getGstin() != null;
    }
    
    @Override
    public int getOrder() {
        return 10;
    }
}
