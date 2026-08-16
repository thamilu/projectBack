package com.eshop.app.seller.api.response;

import com.eshop.app.seller.shared.domain.enums.KycVerificationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response DTO representing seller bank account information.
 *
 * <p><strong>CRITICAL SECURITY REQUIREMENT:</strong> Sensitive bank account numbers must be masked
 * in the mapping layer before constructing this DTO (e.g. XXXXXXXX1234) to prevent financial data leaks.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller bank account verification details")
public class SellerBankAccountResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Bank account record ID. */
    @Schema(description = "Bank account record ID", example = "1")
    private Long id;

    /** Registered account holder name. */
    @Schema(description = "Registered name of the bank account holder.", example = "Ravi Kumar")
    private String accountHolderName;

    /** Masked bank account number. */
    @Schema(description = "Masked bank account number. Raw values must never reach this DTO.",
            example = "XXXXXXXX1234")
    private String accountNumber;

    /** Bank IFSC code. */
    @Schema(description = "IFSC routing code of the bank branch.", example = "SBIN0001234")
    private String ifscCode;

    /** Name of the bank. */
    @Schema(description = "Name of the banking institution.", example = "State Bank of India")
    private String bankName;

    /** Flag indicating primary settlement account status. */
    @Schema(description = "Indicates if this is the primary settlement account (true = primary, false = secondary).",
            example = "true")
    private Boolean isPrimary;

    /** KYC verification status of the bank account. */
    @Schema(description = "Verification status of the bank account.", example = "VERIFIED")
    private KycVerificationStatus verificationStatus;
}
