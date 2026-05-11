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
import lombok.extern.slf4j.Slf4j;

import com.eshop.app.entity.SellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eshop.app.enums.UserRole;


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
        
        if (request.getPincode() != null) {
            com.eshop.app.entity.UserProfile up = user.getUserProfile();
            com.eshop.app.entity.UserAddress address = up.getAddresses().stream()
                .filter(ua -> ua.getIsDefault() != null && ua.getIsDefault())
                .findFirst().orElseGet(() -> {
                    com.eshop.app.entity.UserAddress ua = new com.eshop.app.entity.UserAddress();
                    ua.setUserProfile(up); ua.setIsDefault(true);
                    up.getAddresses().add(ua); return ua;
                });
            address.setTaluk(request.getTaluk());
            address.setDistrict(request.getDistrict());
            address.setState(request.getState());
            address.setPincode(request.getPincode());
        }

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
            tempProfile.setTaluk(request.getTaluk());
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
               request.getDistrict() != null || request.getTaluk() != null ||
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
        log.info("Creating first-time local user from Keycloak ID: {}", keycloakId);
        // Pass minimal info, allowing sync to fill in the rest if needed later
        return syncUserFromKeycloak(keycloakId, null, firstName, lastName, phoneNumber, false);
    }

    @Override
    @Transactional
    public Long syncUserFromKeycloak(String keycloakId, String email, String firstName,
            String lastName, String phoneNumber, Boolean emailVerified) {
        
        String tempId = keycloakId;
        if (tempId == null || tempId.isBlank()) {
            // [HARDEN] Resilience: Fallback to email as surrogate Keycloak ID if 'sub' is missing
            if (email != null && !email.isBlank()) {
                tempId = "fallback:email:" + email;
            } else {
                log.error("Cannot sync user: Keycloak Subject (sub) is null and no email provided");
                throw new IllegalArgumentException("User identifier (sub or email) is mandatory for synchronization");
            }
            log.warn("[HARDEN] Keycloak subject is missing. Using surrogate identity: {}", tempId);
        }
        final String resolvedKeycloakId = tempId;


        // 1. Primary Lookup by Keycloak ID
        java.util.Optional<User> userBySub = userRepository.findByKeycloakId(resolvedKeycloakId);
        User user = null;

        if (userBySub.isPresent()) {
            user = userBySub.get();
            log.debug("Found existing user by Keycloak ID: {} (id: {})", resolvedKeycloakId, user.getId());
        } else {
            // 2. Secondary Lookup by Email
            if (email != null && !email.isBlank()) {
                java.util.List<User> usersByEmail = userRepository.findByEmail(email);
                if (!usersByEmail.isEmpty()) {
                    // Prioritize user with no Keycloak ID or matching Keycloak ID
                    user = usersByEmail.stream()
                            .filter(u -> u.getKeycloakId() == null || u.getKeycloakId().equals(resolvedKeycloakId))
                            .findFirst()
                            .orElse(usersByEmail.getFirst());
                    log.info("Found {} users by email: {}. Selected user id: {}", usersByEmail.size(), email, user.getId());
                }
            }

            // Adoption Logic
            if (user != null) {
                if (user.getKeycloakId() == null) {
                    log.info("Adopting existing user (id: {}) -> Keycloak ID: {}", user.getId(), resolvedKeycloakId);
                    user.setKeycloakId(resolvedKeycloakId);
                } else if (!user.getKeycloakId().equals(resolvedKeycloakId)) {
                    log.warn("IDENTITY DRIFT: Found user (id: {}) with email {} but different Keycloak ID (old: {}, new: {}). Updating identity link.", 
                        user.getId(), email, user.getKeycloakId(), resolvedKeycloakId);
                    user.setKeycloakId(resolvedKeycloakId);
                }
            }
        }

        if (user != null) {
            // Update core fields with truncation
            if (email != null && !email.isBlank()) user.setEmail(truncate(email, 150));
            if (emailVerified != null) user.setEmailVerified(emailVerified);
            
            log.debug("Updating user profile for user ID: {}", user.getId());
            profileSyncService.ensureProfileExists(user, 
                truncate(firstName, 100), 
                truncate(lastName, 100), 
                truncate(phoneNumber, 20), 
                null, null, null, null);
        } else {
            // Create new user
            log.info("Creating new local user record for Keycloak ID: {} (email: {})", resolvedKeycloakId, email);
            
            user = User.builder()
                    .keycloakId(resolvedKeycloakId)
                    .email(truncate(email, 150))
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .role(UserRole.CUSTOMER)
                    .build();
            
            log.debug("Creating associated profile for new user...");
            profileSyncService.ensureProfileExists(user, 
                truncate(firstName, 100), 
                truncate(lastName, 100), 
                truncate(phoneNumber, 20), 
                null, null, null, null);
        }

        try {
            log.debug("Saving user entity to database...");
            user = userRepository.saveAndFlush(user);
            log.info("Successfully synced user identity for: {} (localId: {})", user.getEmail(), user.getId());
            return user.getId();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // [HARDEN] Race-condition recovery: another thread may have already persisted this user.
            // Attempt a read-after-write-failure before throwing, to avoid a false 500.
            log.warn("[HARDEN] DataIntegrityViolation for sub={}, email={}. Attempting recovery read...", keycloakId, email);
            java.util.Optional<User> recovered = userRepository.findByKeycloakId(keycloakId);
            if (recovered.isEmpty() && email != null && !email.isBlank()) {
                recovered = userRepository.findByEmail(email).stream().findFirst();
            }
            if (recovered.isPresent()) {
                log.info("[HARDEN] Recovery successful: resolved user id={} after constraint collision.", recovered.get().getId());
                return recovered.get().getId();
            }
            log.error("DATA INTEGRITY ERROR: Could not recover. Constraint violation: {}. [sub={}, email={}]",
                e.getMostSpecificCause().getMessage(), keycloakId, email);
            throw new com.eshop.app.exception.BusinessException("Identity conflict: email already taken", "IDENTITY_CONFLICT", org.springframework.http.HttpStatus.CONFLICT);
        } catch (Exception e) {
            log.error("FATAL: Failed to persist user identity during sync: {}. Detailed data: [sub={}, email={}]",
                e.getMessage(), keycloakId, email);
            throw e;
        }
    }

    private String truncate(String val, int length) {
        if (val == null) return null;
        return val.length() > length ? val.substring(0, length) : val;
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
