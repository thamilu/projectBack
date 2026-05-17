package com.eshop.app.seller.application.service.impl;

import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.seller.application.mapper.SellerMapper;
import com.eshop.app.seller.application.port.in.SellerAdminUseCase;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import com.eshop.app.user.application.service.KeycloakService;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.entity.Role;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;

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
public class SellerAdminUseCaseImpl implements SellerAdminUseCase {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final KeycloakService keycloakService;
    private final AppProperties appProperties;
    private final SellerMapper sellerMapper;
    private final com.eshop.app.store.application.mapper.StoreMapper storeMapper;

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
        return Arrays.stream(com.eshop.app.seller.shared.domain.enums.SellerBusinessType.values())
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
        if (user.getRole() != Role.SELLER) {
            user.setRole(Role.SELLER);
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

        boolean isFallbackId = keycloakId != null
                && (keycloakId.startsWith("fallback:") || keycloakId.startsWith("unknown-"));

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

    private void ensureStoreExists(User user, SellerProfile profile) {
        if (storeRepository.findBySellerProfile_UserId(user.getId()).isEmpty()) {
            Store newStore = storeMapper.toStore(profile);
            storeRepository.save(newStore);
        }
    }
}

