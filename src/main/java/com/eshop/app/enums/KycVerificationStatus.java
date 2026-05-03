package com.eshop.app.enums;

/**
 * Verification status for KYC documents and bank accounts.
 * Separate from generic VerificationStatus for explicit KYC workflow tracking.
 */
public enum KycVerificationStatus {
    PENDING,
    VERIFIED,
    REJECTED
}
