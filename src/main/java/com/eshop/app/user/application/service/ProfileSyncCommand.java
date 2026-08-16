package com.eshop.app.user.application.service;

import java.time.LocalDate;

/**
 * Immutable command object carrying optional user profile fields for a profile sync operation.
 *
 * <p>All fields are optional (nullable). Only non-null fields will be applied during sync.
 * Callers construct this record using the canonical constructor, passing {@code null} for
 * any field that should not be updated.
 *
 * <p>Replaces the 8-parameter method signature on {@link ProfileSyncService#ensureProfileExists}
 * to ensure the interface contract is stable as new profile fields are added.
 *
 * @param firstName         user's first name; {@code null} = do not update
 * @param lastName          user's last name; {@code null} = do not update
 * @param phone             primary phone number; {@code null} = do not update
 * @param alternatePhone    alternate phone number; {@code null} = do not update
 * @param gender            gender string (must be parseable by {@code Gender.fromString});
 *                          {@code null} = do not update
 * @param preferredLanguage preferred language code; {@code null} = do not update
 * @param dateOfBirth       date of birth; {@code null} = do not update
 */
public record ProfileSyncCommand(
        String firstName,
        String lastName,
        String phone,
        String alternatePhone,
        String gender,
        String preferredLanguage,
        LocalDate dateOfBirth) {

    /** Maximum length enforced for all name fields (firstName, lastName) before persistence. */
    public static final int MAX_NAME_LENGTH = 100;

    /** Maximum length enforced for phone/alternatePhone fields before persistence. */
    public static final int MAX_PHONE_LENGTH = 20;

    /** Maximum length enforced for preferredLanguage field before persistence. */
    public static final int MAX_LANGUAGE_LENGTH = 10;
}
