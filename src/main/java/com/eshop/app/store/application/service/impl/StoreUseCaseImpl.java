package com.eshop.app.store.application.service.impl;

import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.DuplicateResourceException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.store.api.request.StoreCreateRequest;
import com.eshop.app.store.api.response.StoreResponse;
import com.eshop.app.store.application.mapper.StoreMapper;
import com.eshop.app.store.application.port.in.StoreUseCase;
import com.eshop.app.store.domain.entity.Store;
import com.eshop.app.store.domain.repository.StoreRepository;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class StoreUseCaseImpl implements StoreUseCase {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final com.eshop.app.user.domain.repository.SellerProfileRepository sellerProfileRepository;
    private final StoreMapper storeMapper;

    public StoreUseCaseImpl(StoreRepository storeRepository,
            UserRepository userRepository,
            com.eshop.app.user.domain.repository.SellerProfileRepository sellerProfileRepository,
            StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.storeMapper = storeMapper;
    }

    private Long getCurrentUserId() {
        try {
            return SecurityUtils.getAuthenticatedUserId();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return null;
        }
    }

    @Override
    public StoreResponse createStore(StoreCreateRequest request) {
        if (storeRepository.existsByStoreName(request.getStoreName())) {
            throw new DuplicateResourceException("Store with name " + request.getStoreName() + " already exists");
        }

        com.eshop.app.user.domain.entity.SellerProfile profile = sellerProfileRepository
                .findByUser_Id(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller profile not found for user: " + request.getSellerId()));

        User seller = profile.getUser();

        if (seller.getRole() != UserRole.SELLER) {
            seller.setRole(UserRole.SELLER);
            userRepository.save(seller);
        }

        if (storeRepository.findBySellerProfile_UserId(seller.getId()).isPresent()) {
            throw new DuplicateResourceException("Seller already has a store");
        }

        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            description = "Welcome to " + request.getStoreName();
        }

        Store store = Store.builder()
                .storeName(request.getStoreName())
                .shopHandle(request.getShopHandle())
                .description(description)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .district(request.getDistrict())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPincode())
                .googleMapsUrl(request.getGoogleMapsUrl())
                .phone(request.getPhone())
                .email(request.getEmail())
                .logoUrl(request.getLogoUrl())
                .sellerProfile(profile)
                .active(true)
                .build();

        store = storeRepository.save(store);
        return storeMapper.toStoreResponse(store);
    }

    @Override
    public StoreResponse updateStore(Long id, StoreCreateRequest request) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));

        if (!store.getStoreName().equals(request.getStoreName()) &&
                storeRepository.existsByStoreName(request.getStoreName())) {
            throw new DuplicateResourceException("Store with name " + request.getStoreName() + " already exists");
        }

        store.setStoreName(request.getStoreName());
        store.setShopHandle(request.getShopHandle());
        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            description = "Welcome to " + request.getStoreName();
        }
        store.setDescription(description);
        store.setAddressLine1(request.getAddressLine1());
        store.setAddressLine2(request.getAddressLine2());
        store.setCity(request.getCity());
        store.setDistrict(request.getDistrict());
        store.setState(request.getState());
        store.setCountry(request.getCountry());
        store.setPostalCode(request.getPincode());
        store.setGoogleMapsUrl(request.getGoogleMapsUrl());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setLogoUrl(request.getLogoUrl());

        store = storeRepository.save(store);
        return storeMapper.toStoreResponse(store);
    }

    @Override
    public void deleteStore(Long id) {
        if (!storeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Store not found with id: " + id);
        }
        storeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(Long id) {
        Store store = storeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found with id: " + id));
        return storeMapper.toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse getMyStore() {
        log.info("Starting store resolution (getMyStore)");

        Long sellerId = getCurrentUserId();
        log.debug("Strategy 1: App User ID resolved as: {}", sellerId);
        if (sellerId != null && sellerId > 0) {
            Store store = storeRepository.findBySellerProfile_UserId(sellerId).orElse(null);
            if (store != null) {
                log.info("Store resolved via Strategy 1 (App User ID: {})", sellerId);
                if (syncMissingStoreData(store)) {
                    store = storeRepository.save(store);
                }
                return storeMapper.toStoreResponse(store);
            }
            log.debug("No store found for App User ID: {}", sellerId);
        }

        String keycloakId = SecurityUtils.getCurrentUserId().orElse(null);
        log.debug("Strategy 2: Keycloak ID (subject) resolved as: {}", keycloakId);
        if (keycloakId != null && !keycloakId.isBlank()) {
            Store store = storeRepository.findBySellerKeycloakId(keycloakId).orElse(null);
            if (store != null) {
                log.info("Store resolved via Strategy 2 (Keycloak ID: {})", keycloakId);
                if (syncMissingStoreData(store)) {
                    store = storeRepository.save(store);
                }
                return storeMapper.toStoreResponse(store);
            }
            log.debug("No store found for Keycloak ID: {}", keycloakId);
        }

        Long identifiedUserId = sellerId;
        if (identifiedUserId == null && keycloakId != null) {
            identifiedUserId = userRepository.findByKeycloakId(keycloakId).map(User::getId).orElse(null);
        }

        if (identifiedUserId != null) {
            final Long finalIdentifiedUserId = identifiedUserId;
            log.info("JIT: Attempting to auto-create store for user ID: {}", finalIdentifiedUserId);
            User user = userRepository.findById(finalIdentifiedUserId)
                    .orElseThrow(
                            () -> new ResourceNotFoundException("User not found with id: " + finalIdentifiedUserId));

            boolean hasSellerRoleInSecurity = java.util.Optional.ofNullable(SecurityUtils.getCurrentAuthentication())
                    .map(auth -> auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().contains("SELLER") || a.getAuthority().contains("ADMIN")))
                    .orElse(false);

            if (hasSellerRoleInSecurity || (user.getRole() != null
                    && (user.getRole().name().contains("SELLER") || user.getRole().name().contains("ADMIN")))) {
                com.eshop.app.user.domain.entity.SellerProfile profile = sellerProfileRepository
                        .findByUser_Id(user.getId())
                        .orElse(null);
                if (profile != null) {
                    boolean profileChanged = false;
                    if (profile.getStatus() == com.eshop.app.seller.shared.domain.enums.SellerStatus.PENDING) {
                        profile.setStatus(com.eshop.app.seller.shared.domain.enums.SellerStatus.ACTIVE);
                        profile.setApprovedBy("keycloak-sync");
                        profile.setApprovedAt(java.time.LocalDateTime.now());
                        profileChanged = true;
                    }
                    if (user.getRole() != UserRole.SELLER) {
                        user.setRole(UserRole.SELLER);
                        userRepository.save(user);
                        log.info("JIT: Synced local User role to SELLER");
                    }
                    if (profileChanged) {
                        sellerProfileRepository.save(profile);
                        log.info("JIT: Synced SellerProfile status to ACTIVE");
                    }

                    Store newStore = storeMapper.toStore(profile);

                    Store saved = storeRepository.save(newStore);
                    log.info("JIT: Successfully created store ID: {} for user: {}", saved.getId(), identifiedUserId);
                    return storeMapper.toStoreResponse(saved);
                }
            }
        }

        throw new ResourceNotFoundException("Store not found for current seller and JIT creation failed");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> getAllStores(Pageable pageable) {
        Page<Store> storePage = storeRepository.findAll(pageable);
        return PageResponse.of(storePage, storeMapper::toStoreResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreResponse> searchStores(String keyword, Pageable pageable) {
        Page<Store> storePage = storeRepository.searchStores(keyword, pageable);
        return PageResponse.of(storePage, storeMapper::toStoreResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalStoreCount() {
        return storeRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public String getStoreNameBySellerId(Long sellerId) {
        Store store = storeRepository.findBySellerProfile_UserId(sellerId)
                .orElse(null);
        return store != null ? store.getStoreName() : "N/A";
    }

    @Override
    @Transactional(readOnly = true)
    public Double getStoreRatingBySellerId(Long sellerId) {
        Store store = storeRepository.findBySellerProfile_UserId(sellerId)
                .orElse(null);
        return store != null && store.getRating() != null ? store.getRating() : 0.0;
    }

    private boolean syncMissingStoreData(Store store) {
        return storeMapper.syncMissingData(store, store.getSellerProfile());
    }
}


