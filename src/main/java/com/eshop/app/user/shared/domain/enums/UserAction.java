package com.eshop.app.user.shared.domain.enums;

/**
 * Represents auditable actions that can be performed on a user account
 * within the eShop platform. Used primarily for audit logging,
 * authorization checks, and action-based business rule dispatch.
 *
 * <p>Note: If persisted via JPA, ensure {@code @Enumerated(EnumType.STRING)}
 * is used to guarantee data stability across enum reordering.</p>
 */
public enum UserAction {

    /** New user account registration or manual account creation. */
    CREATE,

    /** User updated their own profile/account details. */
    SELF_UPDATE,

    /** User's password was changed (self-service or admin-initiated). */
    PASSWORD_CHANGE,

    /** Password reset flow was requested or completed. */
    PASSWORD_RESET,

    /** User's email address was verified. */
    EMAIL_VERIFY,

    /** An administrator updated another user's account details. */
    UPDATE,

    /** User account was permanently deleted from the system. */
    HARD_DELETE,

    /** User account was soft-deleted (marked inactive/deleted, data retained). */
    SOFT_DELETE,

    /** User account was activated. */
    ACTIVATE,

    /** User account was deactivated. */
    DEACTIVATE,

    /** Account was locked due to security policy or admin intervention. */
    LOCK,

    /** Account was unlocked. */
    UNLOCK,

    /** Two-factor authentication (2FA) was enabled. */
    ENABLE_2FA,

    /** Two-factor authentication (2FA) was disabled. */
    DISABLE_2FA,

    /** Seller profile application was approved. */
    SELLER_APPROVE,

    /** Seller profile application was rejected. */
    SELLER_REJECT,

    /** User's role/permissions were changed. */
    ROLE_CHANGE,

    /** Multiple user accounts were activated in a single bulk operation. */
    BULK_ACTIVATE,

    /** Multiple user accounts were deactivated in a single bulk operation. */
    BULK_DEACTIVATE,

    /** Multiple user accounts were deleted in a single bulk operation. */
    BULK_DELETE,

    /** User data was exported (e.g., for reporting or compliance). */
    EXPORT
}


