package com.eshop.app.user.application.port.in;

import com.eshop.app.user.api.request.UserUpdateRequest;
import com.eshop.app.user.api.request.UserSelfUpdateRequest;
import com.eshop.app.user.api.response.UserResponse;
import com.eshop.app.user.shared.domain.enums.UserRole;
import com.eshop.app.core.api.response.BulkOperationResult;

/**
 * Use case for managing user profiles and roles.
 */
public interface ManageUserUseCase {
    UserResponse updateUser(Long id, UserUpdateRequest request);
    UserResponse updateSelf(Long id, UserSelfUpdateRequest request);
    void deleteUser(Long id);
    void hardDeleteUser(Long id);
    void softDeleteUser(Long id);
    UserResponse activateUser(Long id);
    UserResponse deactivateUser(Long id);
    UserResponse changeRole(Long id, UserRole newRole);
    BulkOperationResult bulkActivate(java.util.List<Long> userIds);
    BulkOperationResult bulkDeactivate(java.util.List<Long> userIds);
}


