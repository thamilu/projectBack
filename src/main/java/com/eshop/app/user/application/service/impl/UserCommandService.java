package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.api.response.BulkOperationResult;
import com.eshop.app.core.audit.Auditable;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.infrastructure.config.cache.CacheConfig;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import com.eshop.app.inventory.shared.domain.enums.AuditAction;
import com.eshop.app.user.api.request.UserSelfUpdateRequest;
import com.eshop.app.user.api.request.UserUpdateRequest;
import com.eshop.app.user.api.response.UserResponse;
import com.eshop.app.user.application.mapper.UserMapper;
import com.eshop.app.user.application.port.in.ManageUserUseCase;
import com.eshop.app.user.application.service.ProfileSyncCommand;
import com.eshop.app.user.application.service.ProfileSyncService;
import com.eshop.app.user.application.service.UserSecurityService;
import com.eshop.app.user.domain.entity.SellerAddress;
import com.eshop.app.user.domain.entity.SellerProfile;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.infrastructure.security.SelfProtectedOperation;
import com.eshop.app.user.infrastructure.security.UserSelfProtectionGuard;
import com.eshop.app.user.shared.domain.enums.UserRole;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class UserCommandService implements ManageUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ProfileSyncService profileSyncService;
    private final UserSecurityService securityService;
    private final UserSelfProtectionGuard selfProtectionGuard;

    @Override
    @PreAuthorize(IS_ADMIN_OR_SELF)
    @CachePut(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(
            action = AuditAction.USER_UPDATE,
            entityType = "User",
            logArgs = true,
            logResult = true)
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanModifyUser(id, auth);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        if (user.getUserProfile() == null) {
            user.setUserProfile(com.eshop.app.user.domain.entity.UserProfile.builder().build());
            user.getUserProfile().setUser(user);
        }
        user.getUserProfile().updatePersonalDetails(request.getFirstName(), request.getLastName());
        user.getUserProfile().setPhone(request.getPhone());

        if (request.getPincode() != null) {
            com.eshop.app.user.domain.entity.UserProfile up = user.getUserProfile();
            com.eshop.app.user.domain.entity.UserAddress address =
                    up.getAddresses().stream()
                            .filter(ua -> ua.getIsDefault() != null && ua.getIsDefault())
                            .findFirst()
                            .orElseGet(
                                    () -> {
                                        com.eshop.app.user.domain.entity.UserAddress ua =
                                                com.eshop.app.user.domain.entity.UserAddress.create(
                                                        up, null, null, null, null, null, null,
                                                        null, null, true);
                                        up.addAddress(ua);
                                        return ua;
                                    });
            address.updateAddressDetails(
                    null,
                    null,
                    null,
                    request.getDistrict(),
                    request.getTaluk(),
                    request.getState(),
                    request.getPincode(),
                    null);
        }

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    @CachePut(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(
            action = AuditAction.USER_UPDATE,
            entityType = "User",
            logArgs = true,
            logResult = true)
    public UserResponse updateSelf(Long id, UserSelfUpdateRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanModifyUser(id, auth);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        profileSyncService.ensureProfileExists(
                user,
                new ProfileSyncCommand(
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone(),
                        request.getAlternatePhone(),
                        request.getGender(),
                        request.getPreferredLanguage(),
                        request.getDateOfBirth()));

        if (hasAddressInfo(request)) {
            SellerAddress tempAddress =
                    SellerAddress.create(
                            request.getAddressLine1(),
                            request.getAddressLine2(),
                            request.getCity(),
                            request.getDistrict(),
                            request.getTaluk(),
                            request.getState(),
                            request.getPincode(),
                            request.getCountry());
            SellerProfile tempProfile =
                    SellerProfile.builder()
                            .addressLine1(tempAddress.getAddressLine1())
                            .addressLine2(tempAddress.getAddressLine2())
                            .city(tempAddress.getCity())
                            .district(tempAddress.getDistrict())
                            .taluk(tempAddress.getTaluk())
                            .state(tempAddress.getState())
                            .pincode(tempAddress.getPincode())
                            .country(tempAddress.getCountry())
                            .build();
            profileSyncService.syncSellerAddressToUser(user, tempProfile);
        }

        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    private boolean hasAddressInfo(UserSelfUpdateRequest request) {
        return request.getAddressLine1() != null
                || request.getCity() != null
                || request.getDistrict() != null
                || request.getTaluk() != null
                || request.getPincode() != null
                || request.getState() != null;
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(action = AuditAction.USER_DELETE, entityType = "User", logArgs = true)
    public void deleteUser(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanModifyUser(id, auth);
        selfProtectionGuard.preventSelfOperation(
                id, securityService.extractUserId(auth), SelfProtectedOperation.DELETE);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        user.softDelete(auth.getName());
        userRepository.save(user);
    }

    @Override
    @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(action = AuditAction.USER_DELETE, entityType = "User", logArgs = true)
    public void hardDeleteUser(Long id) {
        deleteUser(id);
    }

    @Override
    @CacheEvict(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(action = AuditAction.USER_DELETE, entityType = "User", logArgs = true)
    public void softDeleteUser(Long id) {
        deactivateUser(id);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CachePut(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(
            action = AuditAction.ADMIN_USER_UPDATE,
            entityType = "User",
            logArgs = true,
            logResult = true)
    public UserResponse activateUser(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Only administrators can activate users");
        }
        selfProtectionGuard.preventSelfOperation(
                id, securityService.extractUserId(auth), SelfProtectedOperation.ACTIVATE);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        user.activate();
        user = userRepository.save(user);
        log.info("Successfully activated user {} locally, event registered", id);
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CachePut(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(
            action = AuditAction.ADMIN_USER_UPDATE,
            entityType = "User",
            logArgs = true,
            logResult = true)
    public UserResponse deactivateUser(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new AccessDeniedException("Only administrators can deactivate users");
        }
        selfProtectionGuard.preventSelfOperation(
                id, securityService.extractUserId(auth), SelfProtectedOperation.DEACTIVATE);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        user.softDelete(auth.getName());
        user = userRepository.save(user);
        log.info("Successfully deactivated user {} locally, event registered", id);
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CachePut(value = CacheConfig.USERS_CACHE, key = "#id")
    @Auditable(
            action = AuditAction.ADMIN_ROLE_ASSIGN,
            entityType = "User",
            logArgs = true,
            logResult = true)
    public UserResponse changeRole(Long id, UserRole newRole) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanChangeRole(auth);
        selfProtectionGuard.preventSelfOperation(
                id, securityService.extractUserId(auth), SelfProtectedOperation.ROLE_CHANGE);

        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "User not found with id: " + id));

        user.updateRole(newRole);
        user = userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true)
    @Auditable(action = AuditAction.ADMIN_USER_UPDATE, entityType = "User", logArgs = true)
    public BulkOperationResult bulkActivate(List<Long> userIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanPerformBulkOperation(userIds.size(), auth);
        selfProtectionGuard.preventSelfOperationInBulk(
                userIds, securityService.extractUserId(auth), SelfProtectedOperation.ACTIVATE);

        return performBulkStatusChange(userIds, true);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true)
    @Auditable(action = AuditAction.ADMIN_USER_UPDATE, entityType = "User", logArgs = true)
    public BulkOperationResult bulkDeactivate(List<Long> userIds) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        securityService.checkCanPerformBulkOperation(userIds.size(), auth);
        selfProtectionGuard.preventSelfOperationInBulk(
                userIds, securityService.extractUserId(auth), SelfProtectedOperation.DEACTIVATE);

        return performBulkStatusChange(userIds, false);
    }

    private BulkOperationResult performBulkStatusChange(List<Long> userIds, boolean enabled) {
        if (userIds == null || userIds.isEmpty()) {
            return BulkOperationResult.builder().build();
        }

        List<User> users = userRepository.findAllByIdWithDetails(userIds);
        Map<Long, User> userMap =
                users.stream()
                        .filter(u -> u != null)
                        .collect(Collectors.toMap(u -> u.getId(), Function.identity()));

        List<Long> notFound = userIds.stream().filter(id -> !userMap.containsKey(id)).toList();

        List<Long> successful = new ArrayList<>();
        List<Long> failed = new ArrayList<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operator = auth != null ? auth.getName() : "system";

        for (User user : users) {
            try {
                if (enabled) {
                    user.activate();
                } else {
                    user.softDelete(operator);
                }
                userRepository.save(user);
                successful.add(user.getId());
            } catch (Exception e) {
                log.error(
                        "Failed to update status locally for user {}: {}",
                        user.getId(),
                        e.getMessage());
                failed.add(user.getId());
            }
        }

        List<Long> allFailed = new ArrayList<>(failed);
        allFailed.addAll(notFound);

        log.info(
                "Bulk {} completed locally: {} succeeded, {} failed (including {} not found)",
                enabled ? "activation" : "deactivation",
                successful.size(),
                failed.size(),
                notFound.size());

        return BulkOperationResult.builder()
                .totalProcessed(userIds.size())
                .successCount(successful.size())
                .failedCount(allFailed.size())
                .failedIds(allFailed)
                .build();
    }
}
