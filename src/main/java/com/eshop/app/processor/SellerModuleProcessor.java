package com.eshop.app.processor;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.entity.SellerProfile;

/**
 * Strategy interface for processing specific seller profile modules during registration.
 * 
 * <p>Implementations are responsible for building/updating specific sub-entities 
 * such as KYC, Bank Accounts, Farmer Details, etc.
 */
public interface SellerModuleProcessor {

    /**
     * Processes the registration request and updates the seller profile sub-entities.
     *
     * @param profile the seller profile to update
     * @param request the registration request containing data for this module
     */
    void process(SellerProfile profile, SellerRegisterRequest request);

    /**
     * Determines if this processor should be executed for the given request.
     *
     * @param request the registration request
     * @return true if the module is applicable, false otherwise
     */
    boolean isApplicable(SellerRegisterRequest request);
    
    /**
     * Returns the execution order of this processor.
     * Lower values execute first.
     *
     * @return the order value
     */
    default int getOrder() {
        return 0;
    }
}
