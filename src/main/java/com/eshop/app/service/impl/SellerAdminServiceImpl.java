package com.eshop.app.service.impl;

import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.entity.SellerProfile;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.enums.SellerStatus;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.SellerMapper;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.KeycloakService;
import com.eshop.app.service.SellerAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;



@Service
@Slf4j
@RequiredArgsConstructor
public class SellerAdminServiceImpl implements SellerAdminService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final KeycloakService keycloakService;
    private final AppProperties appProperties;
    private final SellerMapper sellerMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SellerProfileResponse> getPendingSellers() {
        List<SellerProfile> pending = sellerProfileRepository.findAllPendingWithDetails();
        log.info("DIAGNOSTIC: Found {} pending seller registration requests in database.", pending.size());
        return pending.stream()
                .map(sellerMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "sellerBusinessTypes")
    public List<Map<String, String>> getBusinessTypes() {
        return Arrays.stream(com.eshop.app.enums.SellerBusinessType.values())
                .map(type -> Map.of("code", type.name(), "label", type.getDisplayName()))
                .toList();
    }

    @Override
    @Transactional
    public void approveSeller(Long sellerId, String processedBy) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setStatus(SellerStatus.ACTIVE);
        profile.setApprovedBy(processedBy);
        profile.setApprovedAt(LocalDateTime.now());
        sellerProfileRepository.save(profile);

        assignKeycloakRole(profile);

        User user = profile.getUser();
        if (user.getRole() != com.eshop.app.enums.UserRole.SELLER) {
            user.setRole(com.eshop.app.enums.UserRole.SELLER);
            userRepository.save(user);
        }

        ensureStoreExists(user, profile);
    }

    @Override
    @Transactional
    public void rejectSeller(Long sellerId, String rejectionReason, String processedBy) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        profile.setStatus(SellerStatus.REJECTED);
        profile.setRejectionReason(rejectionReason);
        profile.setApprovedBy(processedBy);
        profile.setApprovedAt(LocalDateTime.now());
        sellerProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public void syncSellerRole(Long id) {
        SellerProfile profile = sellerProfileRepository.findById(id)
                .or(() -> sellerProfileRepository.findByUser_Id(id))
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        assignKeycloakRole(profile);
    }

    private void assignKeycloakRole(SellerProfile profile) {
        String roleName = appProperties.getSecurity().getRoles().getSeller();
        String keycloakId = profile.getUser().getKeycloakId();
        
        // [HARDEN] Resilience: If the Keycloak ID is missing OR is a fallback/seeded ID 
        // (starting with 'fallback:' or 'unknown-'), we cannot use it directly for UUID lookup.
        // In these cases, we must resolve the user via their email address instead.
        boolean isFallbackId = keycloakId != null && (keycloakId.startsWith("fallback:") || keycloakId.startsWith("unknown-"));
        
        if (keycloakId != null && !isFallbackId) {
            log.info("Assigning role '{}' using Keycloak UUID: {}", roleName, keycloakId);
            keycloakService.assignRole(keycloakId, roleName);
        } else {
            String email = profile.getUser().getEmail();
            log.info("Keycloak ID is {} ({}). Attempting role assignment by email: {}", 
                keycloakId == null ? "null" : "fallback", keycloakId, email);
            keycloakService.assignRoleByEmail(email, roleName);
        }
    }

    private final com.eshop.app.mapper.StoreMapper storeMapper;

    private void ensureStoreExists(User user, SellerProfile profile) {
        if (storeRepository.findBySellerProfile_UserId(user.getId()).isEmpty()) {
            Store newStore = storeMapper.toStore(profile);
            storeRepository.save(newStore);
        }
    }
}
