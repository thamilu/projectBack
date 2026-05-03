package com.eshop.app.service;

import com.eshop.app.entity.User;
import com.eshop.app.entity.SellerProfile;

/**
 * Reusable service for synchronizing data across User, UserProfile, and UserAddress.
 * Ensures data consistency when updates occur in different modules (e.g., Seller Registration).
 */
public interface ProfileSyncService {
    /**
     * Synchronizes address information from a SellerProfile to the User's primary UserAddress.
     */
    void syncSellerAddressToUser(User user, SellerProfile profile);
    
    /**
     * Ensures a User has a valid UserProfile and populates basic info.
     */
    void ensureProfileExists(User user, String firstName, String lastName, String phone, 
                            String alternatePhone, String gender, String preferredLanguage, 
                            java.time.LocalDate dateOfBirth);
}
