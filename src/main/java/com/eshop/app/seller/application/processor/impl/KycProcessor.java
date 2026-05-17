package com.eshop.app.seller.application.processor.impl;



import com.eshop.app.seller.application.processor.SellerModuleProcessor;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.domain.entity.SellerKYC;
import com.eshop.app.user.domain.entity.SellerProfile;
import org.springframework.stereotype.Component;

/**
 * [HARDEN] KYC registration module processor.
 * Handles PAN, GSTIN, and Aadhar KYC details.
 * Execution order: 10 (first in chain â€” KYC is the foundation for other processors).
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
        kyc.setAadhar(request.getAadhar());
        kyc.setGstRegistered(request.getGstin() != null && !request.getGstin().isBlank());

        profile.setKyc(kyc);
    }

    @Override
    public boolean isApplicable(SellerRegisterRequest request) {
        return request.getPanNumber() != null || request.getGstin() != null || request.getAadhar() != null;
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
