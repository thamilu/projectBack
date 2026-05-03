package com.eshop.app.service;

import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerStatus;
import com.eshop.app.entity.*;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.SellerMapper;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.repository.StoreRepository;
import com.eshop.app.config.properties.AppProperties;
import com.eshop.app.enums.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
@Slf4j
public class SellerService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final StoreRepository storeRepository;
    private final KeycloakService keycloakService;
    private final AppProperties appProperties;
    private final SellerMapper sellerMapper;
    private final Map<SellerIdentityType, com.eshop.app.strategy.SellerRegistrationValidator> identityValidators;
    private final List<com.eshop.app.strategy.SellerRegistrationValidator> activityValidators;
    private final List<com.eshop.app.processor.SellerModuleProcessor> moduleProcessors;

    public SellerService(SellerProfileRepository sellerProfileRepository,
                         UserRepository userRepository,
                         StoreRepository storeRepository,
                         KeycloakService keycloakService,
                         AppProperties appProperties,
                         SellerMapper sellerMapper,
                         List<com.eshop.app.strategy.SellerRegistrationValidator> validatorList,
                         List<com.eshop.app.processor.SellerModuleProcessor> processorList) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.userRepository = userRepository;
        this.storeRepository = storeRepository;
        this.keycloakService = keycloakService;
        this.appProperties = appProperties;
        this.sellerMapper = sellerMapper;
        
        // Split validators into Identity-based (mapped) and Activity-based (list)
        this.identityValidators = validatorList.stream()
                .filter(v -> v.getSupportedType() != null)
                .collect(java.util.stream.Collectors.toMap(
                    com.eshop.app.strategy.SellerRegistrationValidator::getSupportedType,
                    java.util.function.Function.identity()
                ));
        
        this.activityValidators = validatorList.stream()
                .filter(v -> v.getSupportedType() == null)
                .toList();

        // Sort processors by order
        this.moduleProcessors = processorList.stream()
                .sorted(java.util.Comparator.comparingInt(com.eshop.app.processor.SellerModuleProcessor::getOrder))
                .toList();
    }

    // ═══════════════════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request) {
        if (!request.isAcceptedTerms()) {
            throw new ValidationException("Terms must be accepted", "TERMS_NOT_ACCEPTED");
        }

        log.info("Registering/Updating seller profile for userId: {}", userId);

        Optional<SellerProfile> existingProfile = sellerProfileRepository.findByUser_Id(userId);
        if (existingProfile.isPresent()) {
            SellerProfile p = existingProfile.get();
            if (p.getStatus() != SellerStatus.PENDING) {
                throw new ValidationException("User already has an active or rejected seller profile", "PROFILE_ALREADY_EXISTS");
            }
        }

        // Smart validation based on identity type
        validateByIdentityType(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Build or update profile
        SellerProfile profile = existingProfile.orElseGet(() -> SellerProfile.builder().user(user).build());
        mapToProfile(profile, request);

        // ── 3. Run Modular Processors ──────────────────────────────
        moduleProcessors.stream()
                .filter(p -> p.isApplicable(request))
                .forEach(p -> p.process(profile, request));

        // Ensure UserProfile exists (but don't overwrite phone)
        if (user.getUserProfile() == null) {
            user.setUserProfile(new UserProfile());
            user.getUserProfile().setUser(user);
        }
        
        // Save the phone to the SellerProfile (business contact)
        if (request.getBusinessPhone() != null) {
            profile.setBusinessMobileNumber(request.getBusinessPhone());
        }
        
        userRepository.save(user);

        // ── 4. Sync personal address to UserAddress ──────────────────
        syncToUserAddress(user, profile);

        // Cleanup modules not applicable to the chosen identity type
        cleanupOrphanedModules(profile);

        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Seller profile created/updated with id: {} for userId: {}, status: PENDING", saved.getId(), userId);

        return sellerMapper.toResponse(saved);
    }

    private void mapToProfile(SellerProfile profile, SellerRegisterRequest request) {
        profile.setIdentityType(request.getIdentityType());
        profile.setShopName(request.getShopName());
        profile.setBusinessTypes(request.getBusinessTypes());
        profile.setBusinessName(request.getBusinessName());
        profile.setDescription(request.getDescription());
        profile.setStatus(SellerStatus.PENDING);

        // Address & Location
        profile.setAddressLine1(request.getAddressLine1());
        profile.setAddressLine2(request.getAddressLine2());
        profile.setCity(request.getCity());
        profile.setDistrict(request.getDistrict());
        profile.setState(request.getState());
        profile.setPincode(request.getPincode());
        profile.setCountry(request.getCountry());
        
        // Store / Warehouse Address
        profile.setStoreAddressLine1(request.getStoreAddressLine1());
        profile.setStoreAddressLine2(request.getStoreAddressLine2());
        profile.setStoreCity(request.getStoreCity());
        profile.setStoreDistrict(request.getStoreDistrict());
        profile.setStoreState(request.getStoreState());
        profile.setStorePincode(request.getStorePincode());
        profile.setStoreCountry(request.getStoreCountry());
        profile.setGoogleMapsUrl(request.getGoogleMapsUrl());
    }

    // ═══════════════════════════════════════════════════════════════
    // Smart Validation
    // ═══════════════════════════════════════════════════════════════

    private void validateByIdentityType(SellerRegisterRequest request) {
        // 1. Validate based on legal identity (INDIVIDUAL/BUSINESS)
        SellerIdentityType type = request.getIdentityType();
        com.eshop.app.strategy.SellerRegistrationValidator identityValidator = identityValidators.get(type);
        
        if (identityValidator != null) {
            identityValidator.validate(request);
        }

        // 2. Validate based on business activities (FARMER, etc.)
        activityValidators.forEach(v -> v.validate(request));
    }

    // ═══════════════════════════════════════════════════════════════
    // Profile Read
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public SellerProfileResponse getSellerProfile(Long userId) {
        log.info("Fetching seller profile for userId: {}", userId);

        // Use fetch-join to load User + UserProfile in a single SQL.
        // This ensures personal info (name, phone, etc.) is always populated
        // without a direct FK from seller_profiles to user_profiles.
        Optional<SellerProfile> profileOpt = sellerProfileRepository.findByUserIdWithProfile(userId);
        if (profileOpt.isEmpty()) {
            boolean exists = sellerProfileRepository.existsByUser_Id(userId);
            log.error("Profile NOT FOUND for userId: {}. existsByUser_Id: {}", userId, exists);
            throw new ResourceNotFoundException("Seller profile not found for userId: " + userId);
        }

        SellerProfile profile = profileOpt.get();
        log.info("Profile found for userId: {}, status: {}, id: {}", userId, profile.getStatus(), profile.getId());
        return sellerMapper.toResponse(profile);
    }

    @Transactional
    public SellerProfileResponse getSellerProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);

        if (userId != null) {
            // Fetch with join so User + UserProfile are loaded in one query
            Optional<SellerProfile> byId = sellerProfileRepository.findByUserIdWithProfile(userId);
            if (byId.isPresent()) {
                return sellerMapper.toResponse(byId.get());
            }
        }

        // JIT: Auto-create minimal PENDING profile for SELLER-role users
        boolean hasSellers = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_SELLER")
                        || a.getAuthority().equalsIgnoreCase("SELLER"));
        if (hasSellers && userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                log.info("JIT: Auto-creating seller profile for SELLER-role user id={}", userId);

                String seededDisplayName = null;
                if (user.getUserProfile() != null) {
                    String f = user.getUserProfile().getFirstName();
                    String l = user.getUserProfile().getLastName();
                    String combined = (f != null ? f : "") + (l != null ? " " + l : "");
                    seededDisplayName = combined.trim().isEmpty() ? null : combined.trim();
                }

                SellerProfile jitProfile = SellerProfile.builder()
                        .user(user)
                        .identityType(SellerIdentityType.INDIVIDUAL)
                        .status(SellerStatus.PENDING)
                        .shopName(seededDisplayName != null ? seededDisplayName : (user.getUsername() != null ? user.getUsername() : "Seller " + user.getId()))
                        .description("Welcome to my shop! I am a new seller on the platform.")
                        .build();
                SellerProfile saved = sellerProfileRepository.save(jitProfile);
                // userProfile will be populated on next fetch via the shared user_id FK
                return sellerMapper.toResponse(saved);
            }
        }

        throw new ResourceNotFoundException("Seller profile not found for current user");
    }

    // ═══════════════════════════════════════════════════════════════
    // Profile Update
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request) {
        log.info("Updating seller profile for userId: {}", userId);

        // Use fetch-join so UserProfile is available for the write-back below
        SellerProfile profile = sellerProfileRepository.findByUserIdWithProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found for userId: " + userId));

        // ── 1. Sync personal info to UserProfile ────────────────────────────
        // Data flow: seller_profiles.user_id → users.id → user_profiles.user_id
        // No direct FK from seller_profiles to user_profiles — industry standard.
        if (profile.getUser() != null) {
            UserProfile up = profile.getUser().getUserProfile();
            if (up == null) {
                up = new UserProfile();
                up.setUser(profile.getUser());
                profile.getUser().setUserProfile(up);
            }
            if (request.getFirstName() != null)        up.setFirstName(request.getFirstName());
            if (request.getLastName() != null)         up.setLastName(request.getLastName());
            // Personal phone should not be updated via seller profile update if we want separation
            // if (request.getPhone() != null)            up.setPhone(request.getPhone());
            if (request.getProfileImageUrl() != null)  up.setProfileImageUrl(request.getProfileImageUrl());
            if (request.getGender() != null)           up.setGender(request.getGender());
            if (request.getDateOfBirth() != null)      up.setDateOfBirth(request.getDateOfBirth());
            if (request.getPreferredLanguage() != null) up.setPreferredLanguage(request.getPreferredLanguage());
            
            userRepository.save(profile.getUser()); // cascades to UserProfile
        }

        // ── 2. Update core seller profile fields ────────────────────────────
        if (request.getShopName() != null)     profile.setShopName(request.getShopName());
        if (request.getBusinessName() != null) profile.setBusinessName(request.getBusinessName());
        if (request.getDescription() != null)  profile.setDescription(request.getDescription());
        if (request.getBusinessPhone() != null) profile.setBusinessMobileNumber(request.getBusinessPhone());

        // Address & Location Updates
        if (request.getAddressLine1() != null) profile.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) profile.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null)         profile.setCity(request.getCity());
        if (request.getDistrict() != null)     profile.setDistrict(request.getDistrict());
        if (request.getState() != null)        profile.setState(request.getState());
        if (request.getPincode() != null)      profile.setPincode(request.getPincode());
        if (request.getCountry() != null)      profile.setCountry(request.getCountry());
        
        // Store / Warehouse Address Updates
        if (request.getStoreAddressLine1() != null) profile.setStoreAddressLine1(request.getStoreAddressLine1());
        if (request.getStoreAddressLine2() != null) profile.setStoreAddressLine2(request.getStoreAddressLine2());
        if (request.getStoreCity() != null)         profile.setStoreCity(request.getStoreCity());
        if (request.getStoreDistrict() != null)     profile.setStoreDistrict(request.getStoreDistrict());
        if (request.getStoreState() != null)        profile.setStoreState(request.getStoreState());
        if (request.getStorePincode() != null)      profile.setStorePincode(request.getStorePincode());
        if (request.getStoreCountry() != null)      profile.setStoreCountry(request.getStoreCountry());
        
        if (request.getGoogleMapsUrl() != null) profile.setGoogleMapsUrl(request.getGoogleMapsUrl());

        // ── 3. Update sub-entities ───────────────────────────────────────────
        updateKyc(profile, request);
        updateDocuments(profile, request);
        updateBankAccount(profile, request);
        updateFarmerDetails(profile, request);
        updateBusinessDetails(profile, request);
        updateWholesaleConfig(profile, request);

        profile.setUpdatedBy(userId.toString());
        SellerProfile updated = sellerProfileRepository.save(profile);
        log.info("Seller profile updated for userId: {}", userId);

        // ── 4. Sync personal address to UserAddress ──────────────────
        syncToUserAddress(profile.getUser(), updated);

        // ── 4. Sync with Store if exists ─────────────────────────────────────
        if (profile.getStores() != null && !profile.getStores().isEmpty()) {
            for (com.eshop.app.entity.Store store : profile.getStores()) {
                if (request.getShopName() != null)    store.setStoreName(request.getShopName());
                if (request.getDescription() != null) store.setDescription(request.getDescription());
                
                if (request.getStoreAddressLine1() != null) store.setAddressLine1(request.getStoreAddressLine1());
                if (request.getStoreAddressLine2() != null) store.setAddressLine2(request.getStoreAddressLine2());
                if (request.getStoreCity() != null)         store.setCity(request.getStoreCity());
                if (request.getStoreDistrict() != null)     store.setDistrict(request.getStoreDistrict());
                if (request.getStoreState() != null)        store.setState(request.getStoreState());
                if (request.getStoreCountry() != null)      store.setCountry(request.getStoreCountry());
                if (request.getStorePincode() != null)      store.setPostalCode(request.getStorePincode());
                if (request.getGoogleMapsUrl() != null)     store.setGoogleMapsUrl(request.getGoogleMapsUrl());
                
                store.setUpdatedBy(userId.toString());
                storeRepository.save(store);
            }
        }

        return sellerMapper.toResponse(updated);
    }

    private void updateKyc(SellerProfile profile, SellerProfileUpdateRequest request) {
        String pan = request.getPanNumber();
        if (pan == null && request.getGstin() == null && request.getGstRegistered() == null
                && request.getKycBusinessType() == null) return;

        SellerKYC kyc = profile.getKyc() != null ? profile.getKyc() : new SellerKYC();
        kyc.setSellerProfile(profile);
        if (pan != null) kyc.setPanNumber(pan);
        else if (kyc.getPanNumber() == null) kyc.setPanNumber("PENDING");
        if (request.getPanName() != null) kyc.setPanName(request.getPanName());
        if (request.getGstin() != null) kyc.setGstin(request.getGstin());
        if (request.getGstRegistered() != null) kyc.setGstRegistered(request.getGstRegistered());
        if (request.getKycBusinessType() != null) kyc.setBusinessType(request.getKycBusinessType());
        profile.setKyc(kyc);
    }

    private void updateDocuments(SellerProfile profile, SellerProfileUpdateRequest request) {
        if (profile.getDocuments() == null) profile.setDocuments(new java.util.HashSet<>());

        if (request.getRegistrationProof() != null) {
            updateOrAddDocument(profile, DocumentType.LICENSE, null, request.getRegistrationProof());
        }
        if (request.getAadhar() != null) {
            updateOrAddDocument(profile, DocumentType.AADHAAR, request.getAadhar(), null);
        }
        String pan = request.getPanNumber();
        if (pan != null) {
            updateOrAddDocument(profile, DocumentType.PAN, pan, null);
        }
    }

    private void updateBankAccount(SellerProfile profile, SellerProfileUpdateRequest request) {
        if (request.getAccountNumber() == null) return;

        if (profile.getBankAccounts() == null) profile.setBankAccounts(new java.util.HashSet<>());
        if (!profile.getBankAccounts().isEmpty()) {
            SellerBankAccount ba = profile.getBankAccounts().iterator().next();
            if (request.getAccountHolderName() != null) ba.setAccountHolderName(request.getAccountHolderName());
            ba.setAccountNumber(request.getAccountNumber());
            if (request.getIfscCode() != null) ba.setIfscCode(request.getIfscCode());
            if (request.getBankName() != null) ba.setBankName(request.getBankName());
        } else {
            profile.getBankAccounts().add(SellerBankAccount.builder()
                    .sellerProfile(profile)
                    .accountHolderName(request.getAccountHolderName())
                    .accountNumber(request.getAccountNumber())
                    .ifscCode(request.getIfscCode())
                    .bankName(request.getBankName())
                    .isPrimary(true)
                    .build());
        }
    }

    private void updateFarmerDetails(SellerProfile profile, SellerProfileUpdateRequest request) {
        if (profile.getBusinessTypes() == null || !profile.getBusinessTypes().contains(com.eshop.app.enums.SellerBusinessType.FARMER)) return;

        SellerFarmerDetails d = profile.getFarmerDetails() != null
                ? profile.getFarmerDetails() : new SellerFarmerDetails();
        d.setSellerProfile(profile);
        if (request.getIsOwnProduce() != null) d.setIsOwnProduce(request.getIsOwnProduce());
        if (request.getFarmLocationVillage() != null) d.setFarmLocation(request.getFarmLocationVillage());
        if (request.getLandArea() != null) d.setLandArea(request.getLandArea());
        if (request.getCropTypes() != null) d.setCropTypes(request.getCropTypes());
        profile.setFarmerDetails(d);
    }

    private void updateBusinessDetails(SellerProfile profile, SellerProfileUpdateRequest request) {
        String bName = request.getBusinessName() != null ? request.getBusinessName() : request.getLegalBusinessName();
        if (bName == null && request.getAuthorizedSignatory() == null
                && request.getWarehouseLocation() == null) return;

        SellerBusinessDetails d = profile.getBusinessDetails() != null
                ? profile.getBusinessDetails() : new SellerBusinessDetails();
        d.setSellerProfile(profile);
        if (bName != null) d.setLegalBusinessName(bName);
        if (request.getAuthorizedSignatory() != null) d.setAuthorizedSignatory(request.getAuthorizedSignatory());
        if (request.getWarehouseLocation() != null) d.setWarehouseLocation(request.getWarehouseLocation());
        profile.setBusinessDetails(d);
    }

    private void updateWholesaleConfig(SellerProfile profile, SellerProfileUpdateRequest request) {
        Boolean enabled = request.getBulkPricingEnabled();
        if (enabled == null && request.getMinOrderQuantity() == null) return;

        SellerWholesaleConfig c = profile.getWholesaleConfig() != null
                ? profile.getWholesaleConfig() : new SellerWholesaleConfig();
        c.setSellerProfile(profile);
        if (enabled != null) c.setBulkPricingEnabled(enabled);
        if (request.getMinOrderQuantity() != null) c.setMinOrderQuantity(request.getMinOrderQuantity());
        profile.setWholesaleConfig(c);
    }

    // ═══════════════════════════════════════════════════════════════
    // Profile Exists Check
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public boolean hasProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return userId != null && sellerProfileRepository.existsByUser_Id(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasProfile(Long userId) {
        return sellerProfileRepository.existsByUser_Id(userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // Admin Methods
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<SellerProfileResponse> getPendingSellers() {
        return sellerProfileRepository.findAllPendingWithDetails().stream()
                .map(sellerMapper::toResponse)
                .toList();
    }

    /**
     * Returns a list of all available business types with their display labels.
     * Results are cached for 1 hour to optimize performance.
     */
    @org.springframework.cache.annotation.Cacheable(value = "sellerBusinessTypes")
    public List<Map<String, String>> getBusinessTypes() {
        return Arrays.stream(com.eshop.app.enums.SellerBusinessType.values())
                .map(type -> Map.of(
                    "code", type.name(), 
                    "label", type.getDisplayName()
                ))
                .toList();
    }

    @Transactional
    public void approveSeller(Long sellerId, String processedBy) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found with id: " + sellerId));

        profile.setStatus(SellerStatus.ACTIVE);
        profile.setApprovedBy(processedBy);
        profile.setApprovedAt(LocalDateTime.now());
        sellerProfileRepository.save(profile);

        // Assign SELLER role in Keycloak with retries
        assignKeycloakRoleWithRetry(profile);

        // Sync local User role
        User user = profile.getUser();
        if (user.getRole() != com.eshop.app.enums.UserRole.SELLER) {
            user.setRole(com.eshop.app.enums.UserRole.SELLER);
            userRepository.save(user);
        }

        // Auto-create Store entity upon approval
        ensureStoreExists(user, profile);
    }

    @Transactional
    public void rejectSeller(Long sellerId, String rejectionReason, String processedBy) {
        SellerProfile profile = sellerProfileRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found with id: " + sellerId));

        profile.setStatus(SellerStatus.REJECTED);
        profile.setRejectionReason(rejectionReason);
        profile.setApprovedBy(processedBy);
        profile.setApprovedAt(LocalDateTime.now());
        sellerProfileRepository.save(profile);
    }

    @Transactional
    public void syncSellerRole(Long id) {
        SellerProfile profile = sellerProfileRepository.findById(id)
                .or(() -> sellerProfileRepository.findByUser_Id(id))
                .orElseThrow(() -> {
                    log.error("Seller profile not found with id or userId: {}", id);
                    return new ResourceNotFoundException("Seller profile not found with id or userId: " + id);
                });

        log.info("Manual Sync Role for Seller ID: {}, User ID: {}", profile.getId(), profile.getUser().getId());

        try {
            if (profile.getUser().getKeycloakId() != null) {
                keycloakService.assignRole(profile.getUser().getKeycloakId(),
                        appProperties.getSecurity().getRoles().getSeller());
            } else {
                keycloakService.assignRoleByUsername(profile.getUser().getUsername(),
                        appProperties.getSecurity().getRoles().getSeller());
            }
            log.info("Role assigned successfully.");
        } catch (Exception e) {
            log.error("Sync Role Failed: ", e);
            throw new RuntimeException("Sync failed: " + e.getMessage());
        }
    }

    /**
     * Remove details that are no longer applicable to the current identity type.
     */
    private void cleanupOrphanedModules(SellerProfile profile) {
        if (profile.getBusinessTypes() == null || !profile.getBusinessTypes().contains(com.eshop.app.enums.SellerBusinessType.FARMER)) {
            if (profile.getFarmerDetails() != null) {
                log.info("Cleaning up orphaned FarmerDetails for seller id: {}", profile.getId());
                profile.setFarmerDetails(null);
            }
        }
        if (profile.getIdentityType() != SellerIdentityType.BUSINESS) {
            if (profile.getBusinessDetails() != null) {
                log.info("Cleaning up orphaned BusinessDetails for seller id: {}", profile.getId());
                profile.setBusinessDetails(null);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    public Long resolveUserId(Authentication authentication) {
        if (authentication == null) {
            throw new ValidationException("Authentication required");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.eshop.app.security.PrincipalDetails pd) {
            return pd.getId();
        }
        throw new ValidationException("Unable to resolve user ID from authentication principal: " +
                (principal != null ? principal.getClass().getName() : "null"));
    }

    private void assignKeycloakRoleWithRetry(SellerProfile profile) {
        log.info("Assigning SELLER role in Keycloak for user: {}", profile.getUser().getUsername());
        
        String roleName = appProperties.getSecurity().getRoles().getSeller();
        if (profile.getUser().getKeycloakId() != null) {
            keycloakService.assignRole(profile.getUser().getKeycloakId(), roleName);
        } else {
            keycloakService.assignRoleByUsername(profile.getUser().getUsername(), roleName);
        }
        
        log.info("Successfully requested role assignment in Keycloak (Resilience4j will handle retries if needed)");
    }

    private void ensureStoreExists(User user, SellerProfile profile) {
        if (storeRepository.findBySellerProfile_UserId(user.getId()).isEmpty()) {
            // Use the user's UserProfile for the seller name
            String fName = profile.getUser() != null && profile.getUser().getUserProfile() != null ? profile.getUser().getUserProfile().getFirstName() : null;
            String lName = profile.getUser() != null && profile.getUser().getUserProfile() != null ? profile.getUser().getUserProfile().getLastName() : null;
            String fullName = (fName != null ? fName : "") + (lName != null ? " " + lName : "");
            fullName = fullName.trim();

            String storeName = !isBlank(profile.getShopName())
                    ? profile.getShopName()
                    : (!isBlank(profile.getBusinessName())
                            ? profile.getBusinessName()
                            : (!fullName.isEmpty()
                                    ? fullName : profile.getUser().getUsername() + "'s Store"));

            Store newStore = Store.builder()
                    .sellerProfile(profile)
                    .storeName(storeName)
                    .description("Welcome to " + storeName)
                    .phone(profile.getBusinessMobileNumber() != null ? profile.getBusinessMobileNumber() : (user.getUserProfile() != null ? user.getUserProfile().getPhone() : null))
                    .addressLine1(profile.getStoreAddressLine1())
                    .addressLine2(profile.getStoreAddressLine2())
                    .city(profile.getStoreCity())
                    .district(profile.getStoreDistrict())
                    .state(profile.getStoreState())
                    .country(profile.getStoreCountry())
                    .postalCode(profile.getStorePincode())
                    .googleMapsUrl(profile.getGoogleMapsUrl())
                    .active(true)
                    .build();
            storeRepository.save(newStore);
            log.info("JIT: Auto-created Store for seller userId: {}", user.getId());
        }
    }

    private void updateOrAddDocument(SellerProfile profile, DocumentType type, String number, String url) {
        for (SellerDocument doc : profile.getDocuments()) {
            if (doc.getDocumentType() == type) {
                if (number != null) doc.setDocumentNumber(number);
                if (url != null) doc.setDocumentUrl(url);
                return;
            }
        }
        profile.getDocuments().add(SellerDocument.builder()
                .sellerProfile(profile)
                .documentType(type)
                .documentNumber(number)
                .documentUrl(url)
                .build());
    }


    private void syncToUserAddress(User user, SellerProfile profile) {
        if (user == null || profile == null) return;
        
        if (user.getUserProfile() == null) {
            UserProfile up = new UserProfile();
            up.setUser(user);
            user.setUserProfile(up);
        }
        
        if (user.getUserProfile().getAddresses() == null) {
            user.getUserProfile().setAddresses(new ArrayList<>());
        }

        UserAddress address = user.getUserProfile().getAddresses().stream()
                .filter(ua -> ua.getIsDefault() != null && ua.getIsDefault())
                .findFirst()
                .orElseGet(() -> {
                    UserAddress ua = new UserAddress();
                    ua.setUserProfile(user.getUserProfile());
                    ua.setIsDefault(true);
                    user.getUserProfile().getAddresses().add(ua);
                    return ua;
                });

        address.setAddressLine1(profile.getAddressLine1());
        address.setAddressLine2(profile.getAddressLine2());
        address.setCity(profile.getCity());
        address.setDistrict(profile.getDistrict());
        address.setState(profile.getState());
        address.setPincode(profile.getPincode());
        address.setCountry(profile.getCountry());
        
        userRepository.save(user);
    }
}
