package com.eshop.app.service.impl;

import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.entity.Store;
import com.eshop.app.entity.User;
import com.eshop.app.enums.UserRole;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.StoreMapper;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.StoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final com.eshop.app.repository.SellerProfileRepository sellerProfileRepository;
    private final StoreMapper storeMapper;

    public StoreServiceImpl(StoreRepository storeRepository,
            UserRepository userRepository,
            com.eshop.app.repository.SellerProfileRepository sellerProfileRepository,
            StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.sellerProfileRepository = sellerProfileRepository;
        this.storeMapper = storeMapper;
    }

    private Long getCurrentUserId() {
        try {
            return com.eshop.app.util.SecurityUtils.getAuthenticatedUserId();
        } catch (org.springframework.security.access.AccessDeniedException e) {
            // Treat "authenticated but unresolvable numeric ID" as unknown.
            // Downstream callers may fall back to keycloakId/email.
            return null;
        }
    }

    @Override
    public StoreResponse createStore(StoreCreateRequest request) {
        if (storeRepository.existsByStoreName(request.getStoreName())) {
            throw new ResourceAlreadyExistsException("Store with name " + request.getStoreName() + " already exists");
        }

        com.eshop.app.entity.SellerProfile profile = sellerProfileRepository.findByUser_Id(request.getSellerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Seller profile not found for user: " + request.getSellerId()));

        User seller = profile.getUser();

        // Some environments rely on Keycloak roles while the local DB role may be
        // stale.
        // Since the controller is already protected with SELLER role, keep local role
        // in sync.
        if (seller.getRole() != UserRole.SELLER) {
            seller.setRole(UserRole.SELLER);
            userRepository.save(seller);
        }

        if (storeRepository.findBySellerProfile_UserId(seller.getId()).isPresent()) {
            throw new ResourceAlreadyExistsException("Seller already has a store");
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
            throw new ResourceAlreadyExistsException("Store with name " + request.getStoreName() + " already exists");
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

        // Strategy 1: Prefer numeric app user ID when available.
        Long sellerId = getCurrentUserId();
        log.debug("Strategy 1: App User ID resolved as: {}", sellerId);
        if (sellerId != null && sellerId > 0) {
            Store store = storeRepository.findBySellerProfile_UserId(sellerId).orElse(null);
            if (store != null) {
                log.info("Store resolved via Strategy 1 (App User ID: {})", sellerId);
                // Self-healing: if store address is blank but profile has it, sync it
                if (syncMissingStoreData(store)) {
                    store = storeRepository.save(store);
                }
                return storeMapper.toStoreResponse(store);
            }
            log.debug("No store found for App User ID: {}", sellerId);
        }

        // Strategy 2: resolve by Keycloak subject (sub/subject) -> seller.keycloakId.
        String keycloakId = com.eshop.app.util.SecurityUtils.getCurrentUserId().orElse(null);
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

        // JIT FIX: As a last resort, if we have identified a user ID but no store was
        // found, create one
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

            // For safety, only auto-create if they have SELLER or ADMIN role in token OR
            // local DB
            boolean hasSellerRoleInSecurity = com.eshop.app.util.SecurityUtils.getCurrentAuthentication()
                    .map(auth -> auth.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().contains("SELLER") || a.getAuthority().contains("ADMIN")))
                    .orElse(false);

            if (hasSellerRoleInSecurity || (user.getRole() != null
                    && (user.getRole().name().contains("SELLER") || user.getRole().name().contains("ADMIN")))) {
                com.eshop.app.entity.SellerProfile profile = sellerProfileRepository.findByUser_Id(user.getId())
                        .orElse(null);
                if (profile != null) {
                    // --- SELF HEALING ---
                    // If the admin assigned the SELLER role manually in Keycloak, sync the local DB
                    boolean profileChanged = false;
                    if (profile.getStatus() == com.eshop.app.enums.SellerStatus.PENDING) {
                        profile.setStatus(com.eshop.app.enums.SellerStatus.ACTIVE);
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

    // Dashboard Analytics Methods Implementation
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
