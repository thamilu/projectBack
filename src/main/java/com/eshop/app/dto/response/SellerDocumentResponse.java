package com.eshop.app.dto.response;

import com.eshop.app.enums.DocumentType;
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
