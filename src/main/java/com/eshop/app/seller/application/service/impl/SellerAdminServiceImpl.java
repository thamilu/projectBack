package com.eshop.app.seller.application.service.impl;

import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.core.exception.infrastructure.KeycloakException;
import com.eshop.app.core.infrastructure.config.properties.AppProperties;
import com.eshop.app.seller.api.response.BusinessTypeResponse;
import com.eshop.app.seller.application.mapper.SellerMapper;
import com.eshop.app.seller.application.port.in.SellerApprovalUseCase;
import com.eshop.app.seller.application.port.in.SellerReferenceDataUseCase;
import com.eshop.app.seller.application.port.in.SellerRoleSyncUseCase;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.application.service.KeycloakService;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.store.application.mapper.StoreMapper;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Service implementation for administering seller registrations, approving applications,
 * synchronising Keycloak access control roles, and serving reference metadata.
 *
 * <p>Implements {@link SellerApprovalUseCase}, {@link SellerReferenceDataUseCase}, and {@link
 * SellerRoleSyncUseCase} following the Interface Segregation Principle.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SellerAdminServiceImpl
        implements SellerApprovalUseCase, SellerReferenceDataUseCase, SellerRoleSyncUseCase {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final KeycloakService keycloakService;
    private final AppProperties appProperties;
    private final SellerMapper sellerMapper;
    private final StoreMapper storeMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SellerProfileResponse> getPendingSellers(Pageable pageable) {
        Assert.notNull(pageable, "pageable must not be null");
        Page<SellerProfile> pending = sellerProfileRepository.findAllPendingWithDetails(pageable);
        log.debug(
                "DIAGNOSTIC: Found {} pending seller registration requests in database page.",
                pending.getNumberOfElements());
        return PageResponse.of(pending, sellerMapper::toResponse);
    }

    @Override
    @Cacheable(value = "sellerBusinessTypes")
    @Transactional(readOnly = true)
    public List<BusinessTypeResponse> getBusinessTypes() {
        return Stream.of(SellerBusinessType.values())
                .map(
                        type ->
                                BusinessTypeResponse.builder()
                                        .code(type.name())
                                        .displayName(type.getDisplayName())
                                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void approveSeller(Long sellerId, String processedBy) {
        Assert.notNull(sellerId, "sellerId must not be null");
        Assert.hasText(processedBy, "processedBy must not be null or blank");

        SellerProfile profile = getSeller(sellerId);

        if (profile.getStatus() != SellerStatus.PENDING) {
            if (profile.getStatus() == SellerStatus.ACTIVE) {
                throw new ValidationException(
                        "Seller profile is already approved", "PROFILE_ALREADY_APPROVED");
            }
            throw new ValidationException(
                    "Seller profile must be in PENDING status to approve", "INVALID_SELLER_STATUS");
        }

        Long approverId = resolveProcessorId(processedBy);

        profile.approveSeller(approverId, processedBy);
        sellerProfileRepository.save(profile);
        log.info(
                "Seller profile {} approved by user {} (Processor ID: {})",
                sellerId,
                processedBy,
                approverId);

        assignKeycloakRole(profile);

        User user = profile.getUser();
        if (user.getRole() != UserRole.SELLER) {
            user.updateRole(UserRole.SELLER);
            userRepository.save(user);
            log.info("Synchronized user role to SELLER for user {}", user.getId());
        }

        ensureStoreExists(user, profile);
    }

    @Override
    @Transactional
    public void rejectSeller(Long sellerId, String rejectionReason, String processedBy) {
        Assert.notNull(sellerId, "sellerId must not be null");
        Assert.hasText(rejectionReason, "rejectionReason must not be null or blank");
        Assert.hasText(processedBy, "processedBy must not be null or blank");

        SellerProfile profile = getSeller(sellerId);

        if (profile.getStatus() != SellerStatus.PENDING) {
            if (profile.getStatus() == SellerStatus.REJECTED) {
                throw new ValidationException(
                        "Seller profile is already rejected", "PROFILE_ALREADY_REJECTED");
            }
            throw new ValidationException(
                    "Seller profile must be in PENDING status to reject", "INVALID_SELLER_STATUS");
        }

        Long rejectorId = resolveProcessorId(processedBy);

        profile.rejectSeller(rejectionReason, rejectorId, processedBy);
        sellerProfileRepository.save(profile);
        log.info(
                "Seller profile {} rejected by user {} (Processor ID: {}) for reason: {}",
                sellerId,
                processedBy,
                rejectorId,
                rejectionReason);
    }

    @Override
    @Transactional
    public void syncSellerRole(Long sellerId) {
        Assert.notNull(sellerId, "sellerId must not be null");

        SellerProfile profile =
                sellerProfileRepository
                        .findById(sellerId)
                        .or(() -> sellerProfileRepository.findByUser_Id(sellerId))
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Seller profile not found"));

        log.info("Synchronizing Keycloak role for seller profile: {}", sellerId);
        assignKeycloakRole(profile);
    }

    private SellerProfile getSeller(Long sellerId) {
        return sellerProfileRepository
                .findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
    }

    private Long resolveProcessorId(String processedBy) {
        return userRepository
                .findByKeycloakId(processedBy)
                .or(() -> userRepository.findByEmail(processedBy).stream().findFirst())
                .map(User::getId)
                .orElseThrow(
                        () ->
                                new ValidationException(
                                        "Processing user not found", "PROCESSOR_NOT_FOUND"));
    }

    private void assignKeycloakRole(SellerProfile profile) {
        String roleName = appProperties.getSecurity().getRoles().getSeller();
        String keycloakId = profile.getUser().getKeycloakId();

        boolean isFallbackId =
                keycloakId != null
                        && (keycloakId.startsWith("fallback:")
                                || keycloakId.startsWith("unknown-"));

        try {
            if (keycloakId != null && !isFallbackId) {
                log.info("Assigning role '{}' using Keycloak UUID: {}", roleName, keycloakId);
                keycloakService.assignRole(keycloakId, roleName);
            } else {
                String email = profile.getUser().getEmail();
                log.info(
                        "Keycloak ID is {} ({}). Attempting role assignment by email: {}",
                        keycloakId == null ? "null" : "fallback",
                        keycloakId,
                        email);
                keycloakService.assignRoleByEmail(email, roleName);
            }
            log.info(
                    "Keycloak role '{}' successfully assigned to seller user {}",
                    roleName,
                    profile.getUser().getId());
        } catch (Exception e) {
            log.error(
                    "Failed to assign Keycloak role '{}' to user {} (Keycloak ID: {}). Triggering"
                            + " transaction rollback.",
                    roleName,
                    profile.getUser().getId(),
                    keycloakId,
                    e);
            throw new KeycloakException(
                    "Keycloak role synchronization failed, transaction rolled back",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e);
        }
    }

    private void ensureStoreExists(User user, SellerProfile profile) {
        if (storeRepository.findBySellerProfile_UserId(user.getId()).isEmpty()) {
            try {
                Store newStore = storeMapper.toStore(profile);
                storeRepository.saveAndFlush(newStore);
                log.info(
                        "Store successfully created for user {} (Seller Profile {})",
                        user.getId(),
                        profile.getId());
            } catch (DataIntegrityViolationException e) {
                log.warn(
                        "Store already exists for user {} (Seller Profile {}) due to concurrent"
                                + " approval: {}",
                        user.getId(),
                        profile.getId(),
                        e.getMessage());
            }
        }
    }
}
