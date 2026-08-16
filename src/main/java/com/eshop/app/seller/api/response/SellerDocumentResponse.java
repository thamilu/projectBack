package com.eshop.app.seller.api.response;

import com.eshop.app.seller.shared.domain.enums.DocumentType;
import com.eshop.app.seller.shared.domain.enums.KycVerificationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing seller document verification information.
 *
 * <p><strong>CRITICAL SECURITY REQUIREMENT:</strong> Sensitive document numbers must be masked
 * in the mapping layer before constructing this DTO. Document URLs must only contain secure, authorized,
 * and time-limited resource locators, never exposing raw internal storage paths.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller document verification details")
public class SellerDocumentResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Seller document record ID. */
    @Schema(description = "Seller document record ID", example = "1")
    private Long id;

    /** Type of identity document. */
    @Schema(description = "Type of identity document.", example = "PAN")
    private DocumentType documentType;

    /** Masked document registration number. */
    @Schema(description = "Masked document registration number. Raw numbers must never reach this DTO.",
            example = "XXXXXX1234")
    private String documentNumber;

    /** Authorized secure document download URL. */
    @Schema(description = "Secure, authorized link to retrieve the document. Never contains raw storage keys or system paths.",
            example = "https://cdn.example.com/documents/abc123")
    private String documentUrl;

    /** Verification status of this specific document. */
    @Schema(description = "Verification status of the document.", example = "VERIFIED")
    private KycVerificationStatus verificationStatus;

    /** Timestamp when document was verified. */
    @Schema(description = "Timestamp when the document was verified in ISO-8601 format (yyyy-MM-dd'T'HH:mm:ss). " +
            "This value is timezone-independent.",
            example = "2026-07-03T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;

    /** Display name of the verifier. */
    @Schema(description = "Display name of the administrator who performed the verification. " +
            "Does not expose internal user IDs or system login usernames.",
            example = "Admin User")
    private String verifiedBy;

    /** Reason for document rejection, if status is REJECTED. */
    @Schema(description = "Explanation for document rejection. Only populated when verification status is REJECTED.",
            example = "Document image is blurred.")
    private String rejectionReason;
}
