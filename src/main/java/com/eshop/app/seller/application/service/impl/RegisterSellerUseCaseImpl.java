package com.eshop.app.seller.application.service.impl;

import com.eshop.app.seller.application.processor.SellerModuleProcessor;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.mapper.SellerMapper;
import com.eshop.app.seller.application.port.in.RegisterSellerUseCase;
import com.eshop.app.seller.shared.domain.enums.SellerIdentityType;
import com.eshop.app.seller.shared.domain.enums.SellerStatus;
import com.eshop.app.seller.shared.domain.enums.SellerBusinessType;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.seller.application.strategy.SellerRegistrationValidator;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.SellerProfileRepository;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.application.service.ProfileSyncService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RegisterSellerUseCaseImpl implements RegisterSellerUseCase {

    private final SellerProfileRepository sellerProfileRepository;
    private final UserRepository userRepository;
    private final SellerMapper sellerMapper;
    private final ProfileSyncService profileSyncService;
    private final Map<SellerIdentityType, SellerRegistrationValidator> identityValidators;
    private final List<SellerRegistrationValidator> activityValidators;
    private final List<SellerModuleProcessor> moduleProcessors;

    public RegisterSellerUseCaseImpl(SellerProfileRepository sellerProfileRepository,
                                       UserRepository userRepository,
                                       SellerMapper sellerMapper,
                                       ProfileSyncService profileSyncService,
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
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        if (!request.isAcceptedTerms()) {
            throw new ValidationException("Terms must be accepted", "TERMS_NOT_ACCEPTED");
        }

        log.info("Registering seller profile for userId: {}", userId);

        Optional<SellerProfile> existingProfile = sellerProfileRepository.findByUser_Id(userId);
        if (existingProfile.isPresent() && existingProfile.get().getStatus() != SellerStatus.PENDING) {
            throw new ValidationException("User already has an active or rejected seller profile", "PROFILE_ALREADY_EXISTS");
        }

        validateRegistration(request);

        String handle = generateOrValidateHandle(request);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SellerProfile profile = existingProfile.orElseGet(() -> SellerProfile.builder().user(user).build());
        
        sellerMapper.updateProfileFromRequest(request, profile);
        profile.setShopHandle(handle);

        moduleProcessors.stream()
                .filter(p -> p.isApplicable(request))
                .forEach(p -> p.process(profile, request));

        profileSyncService.ensureProfileExists(
                user,
                new com.eshop.app.user.application.service.ProfileSyncCommand(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone(),
                        request.getAlternatePhone(),
                        request.getGender(),
                        request.getPreferredLanguage(),
                        request.getDateOfBirth())
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
        if (profile.getBusinessTypes() == null || !profile.getBusinessTypes().contains(SellerBusinessType.FARMER)) {
            profile.setFarmerDetails(null);
        }
        if (profile.getIdentityType() != SellerIdentityType.BUSINESS) {
            profile.setBusinessDetails(null);
        }
    }

    private String generateOrValidateHandle(SellerRegisterRequest request) {
        if (request.getShopHandle() != null && !request.getShopHandle().isBlank()) {
            String customHandle = request.getShopHandle().toLowerCase().replaceAll("[^a-z0-9-]", "-");
            if (sellerProfileRepository.existsByShopHandle(customHandle)) {
                throw new ValidationException("Shop handle already taken. Please choose a different one.", "DUPLICATE_SHOP_HANDLE");
            }
            return customHandle;
        }
        
        String city = request.getCity() != null ? request.getCity() : "";
        String base = (request.getShopName() + "-" + city)
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-");
        
        if (!sellerProfileRepository.existsByShopHandle(base)) {
            return base;
        }
        
        return base + "-" + java.util.UUID.randomUUID().toString().substring(0, 5);
    }
}

