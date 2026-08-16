package com.eshop.app.user.application.service;

import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;

/**
 * Reusable service for synchronizing data across User, UserProfile, and
 * UserAddress.
 * Ensures data consistency when updates occur in different modules (e.g.,
 * Seller Registration).
 */
public interface ProfileSyncService {

    /**
     * Synchronizes address information from a SellerProfile to the User's primary
     * UserAddress.
     *
     * @param user    the target user; silently no-ops if {@code null}
     * @param profile the seller profile providing address data; silently no-ops if {@code null}
     */
    void syncSellerAddressToUser(User user, SellerProfile profile);

    /**
     * Ensures a User has a valid UserProfile and applies non-null fields from the
     * provided {@link ProfileSyncCommand}.
     *
     * <p>Only fields present (non-null) in the command are applied — existing values
     * are not overwritten by null command fields.
     *
     * @param user    the target user; must not be null
     * @param command the profile fields to apply; must not be null
     */
    void ensureProfileExists(User user, ProfileSyncCommand command);
}

