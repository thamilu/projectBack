package com.eshop.app.service.impl;

import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.entity.*;
import com.eshop.app.enums.SellerIdentityType;
import com.eshop.app.enums.SellerStatus;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.mapper.SellerMapper;
import com.eshop.app.processor.SellerModuleProcessor;
import com.eshop.app.repository.SellerProfileRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.SellerRegistrationService;
import com.eshop.app.strategy.SellerRegistrationValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SellerRegistrationServiceImpl implements SellerRegistrationService {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final SellerMapper sellerMapper;
    private final com.eshop.app.service.ProfileSyncService profileSyncService;
    private final Map<SellerIdentityType, SellerRegistrationValidator> identityValidators;
    private final List<SellerRegistrationValidator> activityValidators;
    private final List<SellerModuleProcessor> moduleProcessors;

    public SellerRegistrationServiceImpl(SellerProfileRepository sellerProfileRepository,
                                       UserRepository userRepository,
                                       SellerMapper sellerMapper,
                                       com.eshop.app.service.ProfileSyncService profileSyncService,
                                       List<SellerRegistrationValidator> validatorList,
                                       List<SellerModuleProcessor> processorList) {
        this.sellerProfileRepository = sellerProfileRepository;
        this.userRepository = userRepository;
        this.sellerMapper = sellerMapper;
        this.profileSyncService = profileSyncService;
        
        this.identityValidators = validatorList.stream()
                .filter(v -> v.getSupportedType() != null)
                .collect(Collectors.toMap(SellerRegistrationValidator::getSupportedType, v -> v));
        
        this.activityValidators = validatorList.stream()
                .filter(v -> v.getSupportedType() == null)
                .toList();

        this.moduleProcessors = processorList.stream()
                .sorted(java.util.Comparator.comparingInt(SellerModuleProcessor::getOrder))
                .toList();
    }

    @Override
    @Transactional
    public SellerProfileResponse registerSeller(Long userId, SellerRegisterRequest request) {
        if (!request.isAcceptedTerms()) {
            throw new ValidationException("Terms must be accepted", "TERMS_NOT_ACCEPTED");
        }

        log.info("Registering seller profile for userId: {}", userId);

        Optional<SellerProfile> existingProfile = sellerProfileRepository.findByUser_Id(userId);
        if (existingProfile.isPresent() && existingProfile.get().getStatus() != SellerStatus.PENDING) {
            throw new ValidationException("User already has an active or rejected seller profile", "PROFILE_ALREADY_EXISTS");
        }

        validateRegistration(request);

        // Global Identity: Shop Name is no longer unique, but Shop Handle IS.
        String handle = generateOrValidateHandle(request);
        if (sellerProfileRepository.existsByShopHandle(handle)) {
            throw new ValidationException("Shop handle already taken. Please choose a different one.", "DUPLICATE_SHOP_HANDLE");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SellerProfile profile = existingProfile.orElseGet(() -> SellerProfile.builder().user(user).build());
        
        // Map request to profile using MapStruct
        sellerMapper.updateProfileFromRequest(request, profile);
        profile.setShopHandle(handle); // Force the generated/validated handle

        // Run Modular Processors
        moduleProcessors.stream()
                .filter(p -> p.isApplicable(request))
                .forEach(p -> p.process(profile, request));

        // Sync Address to UserAddress via reusable service
        profileSyncService.ensureProfileExists(
                user,
                request.getFirstName(),
                request.getLastName(),
                request.getPhone(),
                request.getAlternatePhone(),
                request.getGender(),
                request.getPreferredLanguage(),
                request.getDateOfBirth()
        );
        
        profileSyncService.syncSellerAddressToUser(user, profile);
        cleanupOrphanedModules(profile);

        SellerProfile saved = sellerProfileRepository.save(profile);
        return sellerMapper.toResponse(saved);
    }

    private void validateRegistration(SellerRegisterRequest request) {
        SellerRegistrationValidator identityValidator = identityValidators.get(request.getIdentityType());
        if (identityValidator != null) identityValidator.validate(request);
        activityValidators.forEach(v -> v.validate(request));
    }

    private void cleanupOrphanedModules(SellerProfile profile) {
        if (profile.getBusinessTypes() == null || !profile.getBusinessTypes().contains(com.eshop.app.enums.SellerBusinessType.FARMER)) {
            profile.setFarmerDetails(null);
        }
        if (profile.getIdentityType() != SellerIdentityType.BUSINESS) {
            profile.setBusinessDetails(null);
        }
    }

    private String generateOrValidateHandle(SellerRegisterRequest request) {
        if (request.getShopHandle() != null && !request.getShopHandle().isBlank()) {
            return request.getShopHandle().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }
        
        // Auto-generate: shop-name + city
        String base = (request.getShopName() + "-" + request.getCity())
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-");
        
        if (!sellerProfileRepository.existsByShopHandle(base)) {
            return base;
        }
        
        // Fallback: add random suffix
        return base + "-" + java.util.UUID.randomUUID().toString().substring(0, 5);
    }
}
