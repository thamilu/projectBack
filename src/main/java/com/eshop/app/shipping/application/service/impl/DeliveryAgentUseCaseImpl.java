package com.eshop.app.shipping.application.service.impl;

import com.eshop.app.user.application.service.KeycloakService;
import com.eshop.app.user.shared.domain.enums.DeliveryAgentStatus;
import com.eshop.app.shipping.api.request.DeliveryAgentRegisterRequest;
import com.eshop.app.shipping.application.port.in.DeliveryAgentUseCase;
import com.eshop.app.user.domain.repository.DeliveryAgentProfileRepository;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.exception.business.ValidationException;
import com.eshop.app.user.api.response.DeliveryAgentProfileResponse;
import com.eshop.app.user.domain.entity.DeliveryAgentProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAgentUseCaseImpl implements DeliveryAgentUseCase {

    private final DeliveryAgentProfileRepository deliveryAgentRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

    @Override
    @Transactional
    public DeliveryAgentProfileResponse registerDeliveryAgent(Long userId, DeliveryAgentRegisterRequest request) {
        log.info("Registering delivery agent for userId: {}", userId);

        if (deliveryAgentRepository.existsByUser_Id(userId)) {
            throw new ValidationException("User already has a delivery agent profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        DeliveryAgentProfile profile = DeliveryAgentProfile.builder()
                .user(user)
                .vehicleType(request.getVehicleType())
                .licenseNumber(request.getLicenseNumber())
                .zone(request.getZone())
                .status(DeliveryAgentStatus.PENDING)
                .build();

        DeliveryAgentProfile saved = deliveryAgentRepository.save(profile);
        log.info("Delivery agent profile created with id: {}", saved.getId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryAgentProfileResponse> getPendingAgents() {
        return deliveryAgentRepository.findAll().stream()
                .filter(p -> p.getStatus() == DeliveryAgentStatus.PENDING)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void approveAgent(Long agentId) {
        DeliveryAgentProfile profile = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + agentId));

        profile.setStatus(DeliveryAgentStatus.ACTIVE);
        deliveryAgentRepository.save(profile);

        if (profile.getUser().getKeycloakId() != null) {
            keycloakService.assignRole(profile.getUser().getKeycloakId(), "DELIVERY_AGENT");
        } else {
            log.warn("Cannot assign DELIVERY_AGENT role to user {}: Keycloak ID is missing", profile.getUser().getId());
        }
    }

    @Override
    @Transactional
    public void rejectAgent(Long agentId) {
        DeliveryAgentProfile profile = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + agentId));

        profile.setStatus(DeliveryAgentStatus.REJECTED);
        deliveryAgentRepository.save(profile);
    }

    private DeliveryAgentProfileResponse toResponse(DeliveryAgentProfile profile) {
        return DeliveryAgentProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .vehicleType(profile.getVehicleType())
                .licenseNumber(profile.getLicenseNumber())
                .zone(profile.getZone())
                .status(profile.getStatus())
                .build();
    }
}

