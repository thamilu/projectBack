package com.eshop.app.processor.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.entity.SellerBankAccount;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.processor.SellerModuleProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Processor for Seller Bank Account module.
 */
@Component
public class BankProcessor implements SellerModuleProcessor {

    @Override
    public void process(SellerProfile profile, SellerRegisterRequest request) {
        if (request.getAccountNumber() == null) return;

        if (profile.getBankAccounts() == null) {
            profile.setBankAccounts(new java.util.HashSet<>());
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
