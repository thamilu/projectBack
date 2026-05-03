package com.eshop.app.service.impl;

import com.eshop.app.dto.request.UserUpdateRequest;
import com.eshop.app.dto.response.BulkOperationResult;
import com.eshop.app.enums.ExportFormat;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.UserResponse;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.UserMapper;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.UserService;
import com.eshop.app.service.KeycloakService;

import com.eshop.app.entity.SellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eshop.app.enums.UserRole;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeycloakService keycloakService;
    private final com.eshop.app.service.ExportService exportService;
    private final com.eshop.app.service.ProfileSyncService profileSyncService;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper,
            KeycloakService keycloakService, com.eshop.app.service.ExportService exportService,
            com.eshop.app.service.ProfileSyncService profileSyncService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.keycloakService = keycloakService;
        this.exportService = exportService;
        this.profileSyncService = profileSyncService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
        
        log.debug("User fetch debug - id: {}, email: {}, hasProfile: {}", 
            user.getId(), user.getEmail(), user.getUserProfile() != null);
        
        if (user.getUserProfile() != null) {
            log.debug("Profile fetch debug - firstName: {}, lastName: {}", 
                user.getUserProfile().getFirstName(), user.getUserProfile().getLastName());
        }

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getUserProfile() == null) {
            user.setUserProfile(new com.eshop.app.entity.UserProfile());
            user.getUserProfile().setUser(user);
        }
        user.getUserProfile().setFirstName(request.getFirstName());
        user.getUserProfile().setLastName(request.getLastName());
        user.getUserProfile().setPhone(request.getPhone());

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateSelf(Long id, com.eshop.app.dto.request.UserSelfUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Use reusable service to ensure profile exists and sync personal info
        profileSyncService.ensureProfileExists(user, request.getFirstName(), request.getLastName(), request.getPhone(),
                request.getAlternatePhone(), request.getGender(), request.getPreferredLanguage(), request.getDateOfBirth());
        
        // Handle Address Sync if provided
        if (hasAddressInfo(request)) {
            // Map request to temporary SellerProfile for sync (reusing the sync logic)
            SellerProfile tempProfile = new SellerProfile();
            tempProfile.setAddressLine1(request.getAddressLine1() != null ? request.getAddressLine1() : request.getAddress());
            tempProfile.setAddressLine2(request.getAddressLine2());
            tempProfile.setCity(request.getCity());
            tempProfile.setDistrict(request.getDistrict());
            tempProfile.setState(request.getState());
            tempProfile.setPincode(request.getPincode());
            tempProfile.setCountry(request.getCountry());
            
            profileSyncService.syncSellerAddressToUser(user, tempProfile);
        }

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    private boolean hasAddressInfo(com.eshop.app.dto.request.UserSelfUpdateRequest request) {
        return request.getAddress() != null || request.getAddressLine1() != null || request.getCity() != null || 
               request.getPincode() != null || request.getState() != null;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByRole(String role, Pageable pageable) {
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        Page<User> userPage = userRepository.findByRole(userRole, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        Page<User> userPage = userRepository.searchUsers(keyword, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsersByActiveStatus(Boolean active, Pageable pageable) {
        log.warn(
                "Filtering by active status locally is no longer supported as it resides in Keycloak. Returning all users.");
        Page<User> userPage = userRepository.findAll(pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    public UserResponse activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        try {
            keycloakService.setUserEnabled(user.getKeycloakId(), true);
        } catch (Exception e) {
            log.error("Failed to activate user in Keycloak: {}", e.getMessage());
        }
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse deactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        try {
            keycloakService.setUserEnabled(user.getKeycloakId(), false);
        } catch (Exception e) {
            log.error("Failed to deactivate user in Keycloak: {}", e.getMessage());
        }
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse changeRole(Long id, com.eshop.app.enums.UserRole newRole) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        // Map API enum to entity enum
        try {
            UserRole entityRole = UserRole.valueOf(newRole.name());
            user.setRole(entityRole);
        } catch (IllegalArgumentException e) {
            throw new ResourceNotFoundException("Invalid role: " + newRole);
        }
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    public void hardDeleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void softDeleteUser(Long id) {
        // Status is managed by Keycloak, so soft delete is essentially deactivating in
        // Keycloak
        deactivateUser(id);
    }

    /**
     * Shared helper: activate or deactivate a batch of users.
     * Eliminates near-identical bulkActivate / bulkDeactivate bodies.
     */
    @Override
    public BulkOperationResult bulkActivate(java.util.List<Long> userIds) {
        int success = 0;
        java.util.List<Long> failed = new java.util.ArrayList<>();
        for (Long id : userIds) {
            try {
                activateUser(id);
                success++;
            } catch (Exception e) {
                failed.add(id);
            }
        }
        return BulkOperationResult.builder()
                .totalProcessed(userIds.size())
                .successCount(success)
                .failedCount(failed.size())
                .failedIds(failed)
                .build();
    }

    @Override
    public BulkOperationResult bulkDeactivate(java.util.List<Long> userIds) {
        int success = 0;
        java.util.List<Long> failed = new java.util.ArrayList<>();
        for (Long id : userIds) {
            try {
                deactivateUser(id);
                success++;
            } catch (Exception e) {
                failed.add(id);
            }
        }
        return BulkOperationResult.builder()
                .totalProcessed(userIds.size())
                .successCount(success)
                .failedCount(failed.size())
                .failedIds(failed)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportUsers(ExportFormat format, com.eshop.app.enums.UserRole role, Boolean active) {
        log.info("Exporting users with role: {}, format: {}", role, format);
        
        java.util.List<User> users;
        if (role != null) {
            UserRole entityRole = UserRole.valueOf(role.name());
            users = userRepository.findAllByRole(entityRole);
        } else {
            users = userRepository.findAll();
        }

        // Active status filtering is skipped as it resides in Keycloak, 
        // but we've at least limited the set by role if provided.
        
        try {
            String[] headers = new String[] { "Id", "KeycloakId", "FirstName", "LastName", "Role" };
            java.util.List<java.util.Map<String, Object>> data = users.stream().map(u -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("Id", u.getId());
                map.put("KeycloakId", u.getKeycloakId());
                map.put("FirstName", u.getUserProfile() != null ? u.getUserProfile().getFirstName() : "");
                map.put("LastName", u.getUserProfile() != null ? u.getUserProfile().getLastName() : "");
                map.put("Role", u.getRole() != null ? u.getRole().name() : "");
                return map;
            }).toList();

            if (format == ExportFormat.EXCEL) {
                return exportService.exportToExcel("Users", headers, data);
            } else {
                return exportService.exportToCsv(headers, data);
            }
        } catch (Exception e) {
            log.error("Failed to export users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export users", e);
        }
    }

    // --- Private generation methods removed (logic centralized in ExportService) ---

    // Dashboard Analytics Methods Implementation
    @Override
    @Transactional(readOnly = true)
    public long getTotalUserCount() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getCustomerCount() {
        return userRepository.countByRole(UserRole.CUSTOMER);
    }

    @Override
    @Transactional(readOnly = true)
    public long getSellerCount() {
        return userRepository.countByRole(UserRole.SELLER);
    }

    @Override
    @Transactional(readOnly = true)
    public long getDeliveryAgentCount() {
        return userRepository.countByRole(UserRole.DELIVERY_AGENT);
    }

    @Override
    @Transactional(readOnly = true)
    public long getActiveUserCount() {
        // For now, return total count as active status is not in local DB
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long getNewUsersThisMonth() {
        return userRepository.countByCreatedAtAfter(com.eshop.app.util.DateTimeUtils.startOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public java.time.LocalDate getMemberSinceByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : java.time.LocalDate.now();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getUserGrowthData() {
        return userRepository.getUserGrowthData();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<Long> findUserIdByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).map(User::getId);
    }

    @Override
    @Transactional
    public Long createUserFromKeycloak(String keycloakId, String firstName, String lastName, String phoneNumber) {
        return syncUserFromKeycloak(keycloakId, null, null, firstName, lastName, phoneNumber, false);
    }

    @Override
    @Transactional
    public Long syncUserFromKeycloak(String keycloakId, String username, String email, String firstName,
            String lastName, String phoneNumber, Boolean emailVerified) {
        java.util.Optional<User> existingUserOpt = userRepository.findByKeycloakId(keycloakId);

        User user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            // Update core auth fields from Keycloak
            user.setUsername(username);
            user.setEmail(email);
            if (emailVerified != null) user.setEmailVerified(emailVerified);
            
            // Sync profile info using reusable service
            profileSyncService.ensureProfileExists(user, firstName, lastName, phoneNumber, null, null, null, null);
        } else {
            user = User.builder()
                    .keycloakId(keycloakId)
                    .username(username)
                    .email(email)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .role(UserRole.CUSTOMER)
                    .build();
            
            profileSyncService.ensureProfileExists(user, firstName, lastName, phoneNumber, null, null, null, null);
        }

        user = userRepository.save(user);
        return user.getId();
    }

    private UserRole determineBestRole(java.util.Collection<String> roles) {
        if (roles == null || roles.isEmpty())
            return UserRole.CUSTOMER;

        java.util.Set<String> upperRoles = roles.stream()
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());

        if (upperRoles.contains("ADMIN") || upperRoles.contains("ROLE_ADMIN"))
            return UserRole.ADMIN;
        if (upperRoles.contains("SELLER") || upperRoles.contains("ROLE_SELLER"))
            return UserRole.SELLER;
        if (upperRoles.contains("DELIVERY_AGENT") || upperRoles.contains("ROLE_DELIVERY_AGENT"))
            return UserRole.DELIVERY_AGENT;

        return UserRole.CUSTOMER;
    }

    @Override
    @Transactional
    public void syncUserRoles(Long userId, java.util.Collection<String> keycloakRoles) {
        userRepository.findById(userId).ifPresent(user -> {
            UserRole bestRole = determineBestRole(keycloakRoles);
            if (user.getRole() != bestRole) {
                log.info("Syncing role for user ID {}: {} -> {}", user.getId(), user.getRole(), bestRole);
                user.setRole(bestRole);
                userRepository.save(user);
            }
        });
    }

    @Override
    @Transactional
    public void syncKeycloakId(Long userId, String keycloakId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setKeycloakId(keycloakId);
            userRepository.save(user);
            log.info("Synced Keycloak ID {} for user {}", keycloakId, userId);
        });
    }
}
