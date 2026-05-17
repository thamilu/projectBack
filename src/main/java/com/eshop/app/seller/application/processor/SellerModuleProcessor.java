package com.eshop.app.seller.application.processor;


import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.user.domain.entity.SellerProfile;

/**
 * [HARDEN] Strategy interface for modular seller profile sub-entity processors.
 *
 * Moved from root processor/ to seller/application/processor/ â€” proper bounded context ownership.
 * Each implementation handles a specific registration module (KYC, BankAccount, FarmerDetails, etc.)
 *
 * Chain-of-responsibility pattern: processors are auto-discovered, filtered by isApplicable(),
 * and executed in getOrder() sequence.
 */
public interface SellerModuleProcessor {

    /**
     * Processes and updates the seller profile sub-entities from the request.
     *
     * @param profile the seller profile being built/updated
     * @param request the registration request containing module data
     */
    void process(SellerProfile profile, SellerRegisterRequest request);

    /**
     * Guards execution â€” only runs this processor if applicable.
     *
     * @param request the registration request
     * @return true if this processor should be applied
     */
    boolean isApplicable(SellerRegisterRequest request);

    /**
     * Execution order (lower values execute first).
     * Default: 0
     */
    default int getOrder() {
        return 0;
    }
}
