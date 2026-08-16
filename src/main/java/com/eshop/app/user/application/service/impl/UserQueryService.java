package com.eshop.app.user.application.service.impl;

import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.core.infrastructure.config.cache.CacheConfig;
import com.eshop.app.core.util.SearchUtils;
import com.eshop.app.user.api.response.UserResponse;
import com.eshop.app.user.application.dto.UserFilterCriteria;
import com.eshop.app.user.application.mapper.UserMapper;
import com.eshop.app.user.application.port.in.GetUserUseCase;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
@org.springframework.context.annotation.Primary
public class UserQueryService implements GetUserUseCase {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AsyncUserExportService asyncUserExportService;
    private final com.eshop.app.core.util.ExportService exportService;

    @Override
    @Cacheable(value = CacheConfig.USERS_CACHE, key = "#id", unless = "#result == null")
    public UserResponse getUserById(Long id) {
        User user =
                userRepository
                        .findByIdWithDetails(id)
                        .orElseThrow(
                                () -> {
                                    log.warn("User not found with id: {}", id);
                                    return new ResourceNotFoundException(
                                            "User not found with id: " + id);
                                });

        if (log.isDebugEnabled()) {
            log.debug(
                    "User fetch - id: {}, email: {}, hasProfile: {}",
                    user.getId(),
                    maskEmail(user.getEmail()),
                    user.getUserProfile() != null);
        }

        return userMapper.toUserResponse(user);
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<User> userPage = userRepository.findAll(pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    public PageResponse<UserResponse> getAllUsers(UserFilterCriteria criteria, Pageable pageable) {
        Page<User> userPage;
        if (criteria != null && criteria.getActive() != null) {
            userPage = userRepository.findAllByDeleted(!criteria.getActive(), pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    public PageResponse<UserResponse> getUsersByRole(String role, Pageable pageable) {
        UserRole userRole = UserRole.valueOf(role.toUpperCase());
        Page<User> userPage = userRepository.findByRoleAndDeleted(userRole, false, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    public PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable) {
        String sanitized = SearchUtils.sanitize(keyword);
        if (sanitized.isEmpty()) {
            return PageResponse.of(Page.empty(pageable), userMapper::toUserResponse);
        }
        Page<User> userPage = userRepository.searchUsers(sanitized, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    @Override
    public PageResponse<UserResponse> getUsersByActiveStatus(Boolean active, Pageable pageable) {
        boolean deleted = active != null ? !active : false;
        Page<User> userPage = userRepository.findAllByDeleted(deleted, pageable);
        return PageResponse.of(userPage, userMapper::toUserResponse);
    }

    public java.util.concurrent.CompletableFuture<com.eshop.app.user.api.response.ExportResult>
            exportUsersAsync(com.eshop.app.user.api.request.ExportRequest request) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext()
                        .getAuthentication();
        String requestedBy = auth != null ? auth.getName() : "system";
        if (auth != null
                && auth.getPrincipal()
                        instanceof
                        com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails
                                        pd) {
            requestedBy = pd.getEmail();
        }
        return asyncUserExportService.exportUsersAsync(request, requestedBy);
    }

    @Override
    public byte[] exportUsers(com.eshop.app.user.shared.domain.enums.ExportFormat format, UserRole role, Boolean active) {
        log.info("Exporting users with role: {}, format: {}", role, format);

        java.util.List<User> users;
        if (role != null) {
            users = userRepository.findAllByRole(role);
        } else {
            users = userRepository.findAll();
        }

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

            if (format == com.eshop.app.user.shared.domain.enums.ExportFormat.EXCEL) {
                return exportService.exportToExcel("Users", headers, data);
            } else {
                return exportService.exportToCsv(headers, data);
            }
        } catch (Exception e) {
            log.error("Failed to export users: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to export users", e);
        }
    }

    @Override
    public java.time.LocalDate getMemberSinceByUserId(Long userId) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getCreatedAt() != null
                ? user.getCreatedAt().toLocalDate()
                : java.time.LocalDate.now();
    }

    @Override
    public java.util.Optional<Long> findUserIdByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId).map(u -> u.getId());
    }

    public java.util.Optional<java.time.LocalDateTime> getUserUpdatedAt(Long id) {
        return userRepository.findUpdatedAtById(id);
    }

    private String maskEmail(String email) {
        if (email == null) return null;
        int atIdx = email.indexOf("@");
        if (atIdx <= 1) return "***" + (atIdx >= 0 ? email.substring(atIdx) : "");
        return email.charAt(0) + "***" + email.substring(atIdx - 1);
    }
}
