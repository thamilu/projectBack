package com.eshop.app.service.impl;

import com.eshop.app.dto.request.UserUpdateRequest;
import com.eshop.app.dto.request.UserSelfUpdateRequest;
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
import com.eshop.app.enums.DocumentType;
import com.eshop.app.entity.UserProfile;
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

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper,
            KeycloakService keycloakService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.keycloakService = keycloakService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found with id: {}", id);
                    return new ResourceNotFoundException("User not found with id: " + id);
                });
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
    public UserResponse updateSelf(Long id, UserSelfUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getUserProfile() == null) {
            user.setUserProfile(new com.eshop.app.entity.UserProfile());
            user.getUserProfile().setUser(user);
        }
        if (request.getFirstName() != null)
            user.getUserProfile().setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            user.getUserProfile().setLastName(request.getLastName());
        if (request.getPhone() != null)
            user.getUserProfile().setPhone(request.getPhone());

        // Handle Address
        if (request.getAddress() != null || request.getAddressLine1() != null || request.getAddressLine2() != null || 
            request.getCity() != null || request.getPincode() != null || request.getState() != null || request.getDistrict() != null) {
            com.eshop.app.entity.UserAddress address;
            if (user.getUserProfile().getAddresses() == null) {
                user.getUserProfile().setAddresses(new java.util.ArrayList<>());
            }

            if (!user.getUserProfile().getAddresses().isEmpty()) {
                address = user.getUserProfile().getAddresses().get(0);
            } else {
                address = com.eshop.app.entity.UserAddress.builder()
                        .userProfile(user.getUserProfile())
                        .isDefault(true)
                        .build();
                user.getUserProfile().getAddresses().add(address);
            }

            if (request.getAddressLine1() != null)
                address.setAddressLine1(request.getAddressLine1());
            else if (request.getAddress() != null)
                address.setAddressLine1(request.getAddress());

            if (request.getAddressLine2() != null)
                address.setAddressLine2(request.getAddressLine2());
            if (request.getCity() != null)
                address.setCity(request.getCity());
            if (request.getDistrict() != null)
                address.setDistrict(request.getDistrict());
            if (request.getState() != null)
                address.setState(request.getState());
            if (request.getPincode() != null)
                address.setPincode(request.getPincode());
            if (request.getCountry() != null)
                address.setCountry(request.getCountry());
        }

        if (request.getDateOfBirth() != null)
            user.getUserProfile().setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)
            user.getUserProfile().setGender(request.getGender());

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
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
    public byte[] exportUsers(ExportFormat format, com.eshop.app.enums.UserRole role, Boolean active) {
        java.util.List<User> all = userRepository.findAll();
        java.util.List<User> filtered = new java.util.ArrayList<>();
        for (User u : all) {
            if (role != null) {
                try {
                    if (!u.getRole().name().equals(role.name()))
                        continue;
                } catch (Exception ignored) {
                    continue;
                }
            }
            // Active status filter skipped as it's in Keycloak
            filtered.add(u);
        }

        try {
            if (format == ExportFormat.EXCEL) {
                org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
                org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet("Users");
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                String[] cols = new String[] { "Id", "KeycloakId", "FirstName", "LastName", "Role" };
                for (int i = 0; i < cols.length; i++)
                    header.createCell(i).setCellValue(cols[i]);
                int r = 1;
                for (User u : filtered) {
                    org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                    row.createCell(0).setCellValue(u.getId());
                    row.createCell(1).setCellValue(u.getKeycloakId());
                    String fName = (u.getUserProfile() != null && u.getUserProfile().getFirstName() != null)
                            ? u.getUserProfile().getFirstName()
                            : "";
                    String lName = (u.getUserProfile() != null && u.getUserProfile().getLastName() != null)
                            ? u.getUserProfile().getLastName()
                            : "";
                    row.createCell(2).setCellValue(fName);
                    row.createCell(3).setCellValue(lName);
                    row.createCell(4).setCellValue(u.getRole() == null ? "" : u.getRole().name());
                }
                try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
                    wb.write(out);
                    wb.close();
                    return out.toByteArray();
                }
            } else {
                // CSV
                StringBuilder sb = new StringBuilder();
                sb.append("Id,KeycloakId,FirstName,LastName,Role\n");
                for (User u : filtered) {
                    sb.append(u.getId()).append(',')
                            .append('"').append(u.getKeycloakId()).append('"').append(',')
                            .append('"')
                            .append((u.getUserProfile() != null && u.getUserProfile().getFirstName() != null)
                                    ? u.getUserProfile().getFirstName()
                                    : "")
                            .append('"').append(',')
                            .append('"')
                            .append((u.getUserProfile() != null && u.getUserProfile().getLastName() != null)
                                    ? u.getUserProfile().getLastName()
                                    : "")
                            .append('"').append(',')
                            .append(u.getRole() == null ? "" : u.getRole().name()).append('\n');
                }
                return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export users", e);
        }
    }

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
            // Update fields from Keycloak
            user.setUsername(username);
            user.setEmail(email);
            if (emailVerified != null) {
                user.setEmailVerified(emailVerified);
            }
            if (user.getUserProfile() != null) {
                user.getUserProfile().setFirstName(firstName);
                user.getUserProfile().setLastName(lastName);
                if (phoneNumber != null && !phoneNumber.isBlank()) {
                    user.getUserProfile().setPhone(phoneNumber);
                }
            }
        } else {
            user = User.builder()
                    .keycloakId(keycloakId)
                    .username(username)
                    .email(email)
                    .emailVerified(emailVerified != null ? emailVerified : false)
                    .role(UserRole.CUSTOMER)
                    .build();

            UserProfile profile = UserProfile.builder()
                    .user(user)
                    .firstName(firstName)
                    .lastName(lastName)
                    .phone(phoneNumber)
                    .build();
            user.setUserProfile(profile);
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
