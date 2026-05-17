package com.eshop.app.seller.api.response;

import com.eshop.app.seller.shared.domain.enums.DocumentType;

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
public class SellerDocumentResponse {
    private Long id;
    private DocumentType documentType;
    private String documentNumber;
    private String documentUrl;
    private KycVerificationStatus verificationStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;
    private String rejectionReason;
}
