package com.eshop.app.seller.api.response;

import com.eshop.app.seller.shared.domain.enums.KycBusinessType;
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
 * Response DTO representing seller KYC (Know Your Customer) verification data.
 *
 * <p><strong>CRITICAL SECURITY REQUIREMENT:</strong> This DTO must never contain unmasked PAN or Aadhaar values.
 * Masking is mandatory before object construction to prevent accidental exposure of personally identifiable information (PII)
 * at the presentation layer.
 *
 * @since 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Seller KYC verification information")
public class SellerKYCResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** KYC record identifier. */
    @Schema(description = "KYC record ID", example = "1")
    private Long id;

    /** Masked PAN number. Only the last 4 alphanumeric characters are visible. */
    @Schema(description = "Masked PAN number", example = "XXXXX1234A")
    private String panNumber;

    /** Name as registered on PAN card. */
    @Schema(description = "Name on PAN card", example = "John Doe")
    private String panName;

    /** GSTIN (Goods and Services Tax Identification Number). */
    @Schema(description = "GSTIN number", example = "22AAAAA0000A1Z5")
    private String gstin;

    /** Masked Aadhaar number. Only the last 4 digits are visible. */
    @Schema(description = "Masked Aadhaar number", example = "XXXX-XXXX-1234")
    private String aadhar;

    /** Whether the seller is GST registered. */
    @Schema(description = "Whether the seller is GST registered. " +
            "Values: true = GST registered, false = not GST registered, null = information unavailable.",
            example = "true", allowableValues = {"true", "false", "null"})
    private Boolean gstRegistered;

    /** Type of business for KYC classification. */
    @Schema(description = "KYC business type classification. " +
            "Clients should treat this enum as extensible and handle unknown business types gracefully.",
            example = "INDIVIDUAL")
    private KycBusinessType businessType;

    /** Current KYC verification status. */
    @Schema(description = "Current verification status of the KYC record. " +
            "Clients should treat this enum as extensible and handle unknown statuses gracefully.",
            example = "VERIFIED")
    private KycVerificationStatus verificationStatus;

    /** Timestamp when KYC verification was completed. */
    @Schema(description = "KYC verification timestamp in ISO-8601 format (yyyy-MM-dd'T'HH:mm:ss). " +
            "This value is timezone-independent (local timezone of the server).",
            example = "2026-07-03T14:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime verifiedAt;

    /** Display name of the verifier (not an internal identifier). */
    @Schema(description = "Display name of the administrator who performed the verification. " +
            "Never exposes internal system usernames, emails, or database IDs.",
            example = "Admin User")
    private String verifiedBy;
}
