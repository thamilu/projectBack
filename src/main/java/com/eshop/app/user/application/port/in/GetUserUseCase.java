package com.eshop.app.user.application.port.in;

import com.eshop.app.user.api.response.UserResponse;
import com.eshop.app.user.shared.domain.enums.ExportFormat;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Use case for retrieving user information and history.
 */
public interface GetUserUseCase {
    UserResponse getUserById(Long id);
    PageResponse<UserResponse> getAllUsers(Pageable pageable);
    PageResponse<UserResponse> getUsersByRole(String role, Pageable pageable);
    PageResponse<UserResponse> searchUsers(String keyword, Pageable pageable);
    PageResponse<UserResponse> getUsersByActiveStatus(Boolean active, Pageable pageable);
    byte[] exportUsers(ExportFormat format, UserRole role, Boolean active);
    java.time.LocalDate getMemberSinceByUserId(Long userId);
    java.util.Optional<Long> findUserIdByKeycloakId(String keycloakId);
}


