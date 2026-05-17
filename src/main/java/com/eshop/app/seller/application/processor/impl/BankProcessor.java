package com.eshop.app.seller.application.processor.impl;



import com.eshop.app.seller.application.processor.SellerModuleProcessor;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.domain.entity.SellerBankAccount;
import com.eshop.app.user.domain.entity.SellerProfile;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * [HARDEN] Seller Bank Account registration module processor.
 * Handles extraction and persistence of bank account details from the registration request.
 *
 * Moved from root processor/impl/ to seller/application/processor/impl/ â€” proper bounded context.
 * Execution order: 20 (after KYC, before final submission).
 */
@Component
public class BankProcessor implements SellerModuleProcessor {

    @Override
    public void process(SellerProfile profile, SellerRegisterRequest request) {
        if (request.getAccountNumber() == null) return;

        if (profile.getBankAccounts() == null) {
            profile.setBankAccounts(new HashSet<>());
        }

        SellerBankAccount account = profile.getBankAccounts().stream()
            .filter(SellerBankAccount::getIsPrimary)
            .findFirst()
            .orElseGet(() -> {
                SellerBankAccount newAcc = new SellerBankAccount();
                newAcc.setSellerProfile(profile);
                newAcc.setIsPrimary(true);
                profile.getBankAccounts().add(newAcc);
                return newAcc;
            });

        account.setAccountNumber(request.getAccountNumber());
        account.setIfscCode(request.getIfscCode());
    }

    @Override
    public boolean isApplicable(SellerRegisterRequest request) {
        return request.getAccountNumber() != null;
    }

    @Override
    public int getOrder() {
        return 20;
    }
}
