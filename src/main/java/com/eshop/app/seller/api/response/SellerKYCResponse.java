package com.eshop.app.seller.api.response;

import com.eshop.app.seller.shared.domain.enums.KycBusinessType;
import com.eshop.app.seller.shared.domain.enums.KycVerificationStatus;




import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerKYCResponse {
    private Long id;
    private String panNumber;
    private String panName;
    private String gstin;
    private String aadhar;
    private Boolean gstRegistered;
    private KycBusinessType businessType;
    private KycVerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}
