package com.eshop.app.service.impl;

import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.entity.*;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.mapper.SellerMapper;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.service.SellerProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerProfileServiceImpl implements SellerProfileService {

    private final SellerProfileRepository sellerProfileRepository;
    private final StoreRepository storeRepository;
    private final SellerMapper sellerMapper;
    private final com.eshop.app.service.ProfileSyncService profileSyncService;

    @Override
    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(Long userId) {
        log.info("Fetching seller profile for userId: {}", userId);
        SellerProfile profile = sellerProfileRepository.findByUserIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for userId: " + userId));
        return sellerMapper.toResponse(profile);
    }

    @Override
    @Transactional
    public SellerProfileResponse getSellerProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        if (userId == null) throw new ValidationException("Unable to resolve user ID");

        return sellerProfileRepository.findByUserIdWithProfile(userId)
                .map(sellerMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for current user"));
    }

    @Override
    @Transactional
    public SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request) {
        log.info("Updating seller profile for userId: {}", userId);

        SellerProfile profile = sellerProfileRepository.findByUserIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for userId: " + userId));

        if (request.getShopHandle() != null && !request.getShopHandle().equals(profile.getShopHandle())) {
            String sanitizedHandle = request.getShopHandle().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            if (sellerProfileRepository.existsByShopHandleAndIdNot(sanitizedHandle, profile.getId())) {
                throw new ValidationException("Shop handle already taken", "DUPLICATE_SHOP_HANDLE");
            }
            profile.setShopHandle(sanitizedHandle);
        }

        if (request.getShopLogoUrl() != null) {
            profile.setShopLogoUrl(request.getShopLogoUrl());
        }

        // 1. Update UserProfile info via reusable service
        profileSyncService.ensureProfileExists(
                profile.getUser(), 
                request.getFirstName(), 
                request.getLastName(), 
                request.getPhone(), // Pass phone if provided
                request.getAlternatePhone(),
                request.getGender(),
                request.getPreferredLanguage(),
                request.getDateOfBirth()
        );

        // 2. Update core seller profile fields via MapStruct
        sellerMapper.updateProfileFromUpdateRequest(request, profile);

        profile.setUpdatedBy(userId.toString());
        SellerProfile updated = sellerProfileRepository.save(profile);

        // 3. Sync to Store & UserAddress
        syncToStore(updated, request, userId);
        profileSyncService.syncSellerAddressToUser(profile.getUser(), updated);

        return sellerMapper.toResponse(updated);
    }

    @Override
    public boolean hasProfile(Long userId) {
        return sellerProfileRepository.existsByUser_Id(userId);
    }

    @Override
    public boolean hasProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return userId != null && hasProfile(userId);
    }

    @Override
    public boolean existsByShopHandle(String handle) {
        return sellerProfileRepository.existsByShopHandle(handle);
    }

    private void syncToStore(SellerProfile profile, SellerProfileUpdateRequest request, Long userId) {
        if (profile.getStores() == null) return;
        
        for (Store store : profile.getStores()) {
            if (request.getShopName() != null)    store.setStoreName(request.getShopName());
            if (request.getDescription() != null) store.setDescription(request.getDescription());
            if (request.getShopLogoUrl() != null) store.setLogoUrl(request.getShopLogoUrl());
            if (profile.getShopHandle() != null) store.setShopHandle(profile.getShopHandle());
            
            // Sync store address fields if provided in request
            if (request.getStoreAddressLine1() != null) store.setAddressLine1(request.getStoreAddressLine1());
            if (request.getStoreAddressLine2() != null) store.setAddressLine2(request.getStoreAddressLine2());
            if (request.getStoreCity() != null)         store.setCity(request.getStoreCity());
            if (request.getStoreDistrict() != null)     store.setDistrict(request.getStoreDistrict());
            if (request.getStoreState() != null)        store.setState(request.getStoreState());
            if (request.getStorePincode() != null)      store.setPostalCode(request.getStorePincode());
            if (request.getStoreCountry() != null)      store.setCountry(request.getStoreCountry());
            if (request.getGoogleMapsUrl() != null)     store.setGoogleMapsUrl(request.getGoogleMapsUrl());
            
            // Business phone
            if (request.getBusinessPhone() != null)     store.setPhone(request.getBusinessPhone());
            else if (request.getPhone() != null)        store.setPhone(request.getPhone());
            
            store.setUpdatedBy(userId.toString());
            storeRepository.save(store);
        }
    }

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.eshop.app.security.PrincipalDetails pd) {
            return pd.getId();
        }
        return null;
    }
}
