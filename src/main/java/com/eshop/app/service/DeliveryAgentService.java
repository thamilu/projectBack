package com.eshop.app.service;

import com.eshop.app.dto.request.DeliveryAgentRegisterRequest;
import com.eshop.app.dto.response.DeliveryAgentProfileResponse;
import com.eshop.app.entity.DeliveryAgentProfile;
import com.eshop.app.entity.User;
import com.eshop.app.enums.DeliveryAgentStatus;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.exception.ValidationException;
import com.eshop.app.repository.DeliveryAgentRepository;
import com.eshop.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeliveryAgentService {

    private final DeliveryAgentRepository deliveryAgentRepository;
    private final UserRepository userRepository;
    private final KeycloakService keycloakService;

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

    // --- Admin Methods ---

    @Transactional(readOnly = true)
    public List<DeliveryAgentProfileResponse> getPendingAgents() {
        return deliveryAgentRepository.findAll().stream()
                .filter(p -> p.getStatus() == DeliveryAgentStatus.PENDING)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void approveAgent(Long agentId) {
        DeliveryAgentProfile profile = deliveryAgentRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent profile not found with id: " + agentId));

        profile.setStatus(DeliveryAgentStatus.ACTIVE);
        deliveryAgentRepository.save(profile);

        // Assign DELIVERY_AGENT role in Keycloak
        if (profile.getUser().getKeycloakId() != null) {
            keycloakService.assignRole(profile.getUser().getKeycloakId(), "DELIVERY_AGENT");
        } else {
            log.warn("Cannot assign DELIVERY_AGENT role to user {}: Keycloak ID is missing", profile.getUser().getId());
        }
    }

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
