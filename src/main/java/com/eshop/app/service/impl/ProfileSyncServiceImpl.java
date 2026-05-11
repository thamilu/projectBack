package com.eshop.app.service.impl;

import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.User;
import com.eshop.app.entity.UserAddress;
import com.eshop.app.entity.UserProfile;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.ProfileSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProfileSyncServiceImpl implements ProfileSyncService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public void syncSellerAddressToUser(User user, SellerProfile profile) {
        if (user == null || profile == null) return;
        
        UserProfile up = ensureAndGetProfile(user);
        
        if (up.getAddresses() == null) {
            up.setAddresses(new ArrayList<>());
        }

        UserAddress address = up.getAddresses().stream()
                .filter(ua -> ua.getIsDefault() != null && ua.getIsDefault())
                .findFirst()
                .orElseGet(() -> {
                    UserAddress ua = new UserAddress();
                    ua.setUserProfile(up);
                    ua.setIsDefault(true);
                    up.getAddresses().add(ua);
                    return ua;
                });

        address.setAddressLine1(profile.getAddressLine1());
        address.setAddressLine2(profile.getAddressLine2());
        address.setCity(profile.getCity());
        address.setDistrict(profile.getDistrict());
        address.setTaluk(profile.getTaluk());
        address.setState(profile.getState());
        address.setPincode(profile.getPincode());
        address.setCountry(profile.getCountry());
        
        userRepository.save(user);
        log.debug("Synchronized seller address to UserAddress for user: {}", user.getId());
    }

    @Override
    @Transactional
    public void ensureProfileExists(User user, String firstName, String lastName, String phone,
                                   String alternatePhone, String gender, String preferredLanguage,
                                   java.time.LocalDate dateOfBirth) {
        UserProfile up = ensureAndGetProfile(user);
        
        if (firstName != null) up.setFirstName(firstName);
        if (lastName != null)  up.setLastName(lastName);
        if (phone != null)     up.setPhone(phone);
        if (alternatePhone != null) up.setAlternatePhone(alternatePhone);
        if (gender != null)    up.setGender(gender);
        if (preferredLanguage != null) up.setPreferredLanguage(preferredLanguage);
        if (dateOfBirth != null) up.setDateOfBirth(dateOfBirth);
        
        userRepository.save(user);
    }

    private UserProfile ensureAndGetProfile(User user) {
        if (user.getUserProfile() == null) {
            UserProfile up = new UserProfile();
            up.setUser(user);
            user.setUserProfile(up);
            return up;
        }
        return user.getUserProfile();
    }
}
