package com.eshop.app.dto.response;

import com.eshop.app.enums.KycVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerBankAccountResponse {
    private Long id;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private Boolean isPrimary;
    private KycVerificationStatus verificationStatus;
}
