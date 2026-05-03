package com.eshop.app.service;

import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

/**
 * Enterprise Facade for Seller operations.
 * Delegates to specialized services to maintain Single Responsibility Principle (SRP).
 * 
 * @deprecated Use specific services (SellerProfileService, SellerRegistrationService, SellerAdminService) directly in new code.
 */
@RequiredArgsConstructor
public class SellerService implements SellerProfileService, SellerRegistrationService, SellerAdminService {

    private final SellerProfileService profileService;
    private final SellerRegistrationService registrationService;
    private final SellerAdminService adminService;

    /**
     * Helper to extract the local user database ID from the authenticated user.
     * @deprecated Use SecurityUtils directly in new controller code.
     */
    @Deprecated
    public Long resolveUserId(Authentication authentication) {
        return com.eshop.app.util.SecurityUtils.getAuthenticatedUserId();
    }

    // --- Profile Operations ---
    @Override
    public SellerProfileResponse getSellerProfile(Long userId) {
        return profileService.getSellerProfile(userId);
    }

    @Override
    public SellerProfileResponse getSellerProfile(Authentication authentication) {
        return profileService.getSellerProfile(authentication);
    }

    @Override
    public SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request) {
        return profileService.updateSellerProfile(userId, request);
    }

    @Override
    public boolean hasProfile(Long userId) {
        return profileService.hasProfile(userId);
    }

    @Override
    public boolean hasProfile(Authentication authentication) {
        return profileService.hasProfile(authentication);
    }

    // --- Registration Operations ---
    @Override
    public SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request) {
        return registrationService.registerSeller(userId, request);
    }

    // --- Admin Operations ---
    @Override
    public List<SellerProfileResponse> getPendingSellers() {
        return adminService.getPendingSellers();
    }

    @Override
    public List<Map<String, String>> getBusinessTypes() {
        return adminService.getBusinessTypes();
    }

    @Override
    public void approveSeller(Long sellerId, String processedBy) {
        adminService.approveSeller(sellerId, processedBy);
    }

    @Override
    public void rejectSeller(Long sellerId, String rejectionReason, String processedBy) {
        adminService.rejectSeller(sellerId, rejectionReason, processedBy);
    }

    @Override
    public void syncSellerRole(Long id) {
        adminService.syncSellerRole(id);
    }
}
