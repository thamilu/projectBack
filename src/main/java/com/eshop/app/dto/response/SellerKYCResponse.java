package com.eshop.app.dto.response;

import com.eshop.app.enums.KycBusinessType;
import com.eshop.app.enums.KycVerificationStatus;
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
    private Boolean gstRegistered;
    private KycBusinessType businessType;
    private KycVerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
}
