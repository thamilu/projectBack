package com.eshop.app.user.api.controller;

import com.eshop.app.user.shared.domain.enums.ExportFormat;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.inventory.application.service.UserAuditService;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.BulkOperationResult;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.user.api.request.RoleChangeRequest;
import com.eshop.app.user.api.request.UserSelfUpdateRequest;
import com.eshop.app.user.api.request.UserUpdateRequest;
import com.eshop.app.user.api.response.UserResponse;
import com.eshop.app.user.shared.domain.enums.UserAction;
import com.eshop.app.user.shared.domain.enums.UserRole;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * User Management Controller
 * Handles user profile operations, user administration, and user search
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "User Management", description = "User profile and account management endpoints")
@RestController
@RequestMapping(value = ApiConstants.Endpoints.USERS, produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "createdAt", "firstName", "lastName", "role");
    private static final int MAX_PAGE_SIZE = 100;

    private final com.eshop.app.user.application.port.in.ManageUserUseCase manageUserUseCase;
    private final com.eshop.app.user.application.port.in.GetUserUseCase getUserUseCase;
    private final com.eshop.app.user.application.port.in.IdentitySyncUseCase identitySyncUseCase;
    private final UserAuditService auditService;

    // ==================== CURRENT USER ENDPOINTS ====================

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Timed(value = "user.me.get", description = "Time to get current user")
    @Operation(summary = "Get current user profile", description = "Retrieve the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            org.springframework.security.core.Authentication authentication) {

        Long userId = principalDetails.getId();

        // Resilience: If local ID is missing, try a last-resort sync
        if (userId == null || userId == -1L) {
            log.warn("Principal {} has missing local ID. Attempting last-resort resolution.",
                    principalDetails.getEmail());

            if (authentication.getCredentials() instanceof Jwt jwt) {
                try {
                    userId = identitySyncUseCase.syncUserFromKeycloak(
                            jwt.getSubject(),
                            jwt.getClaimAsString("email"),
                            jwt.getClaimAsString("given_name"),
                            jwt.getClaimAsString("family_name"),
                            jwt.getClaimAsString("phone_number"),
                            jwt.getClaim("email_verified"));
                    log.info("[HARDEN] Resolved local identity for user {} as ID: {}", principalDetails.getEmail(),
                            userId);
                } catch (Exception e) {
                    // [HARDEN] Log full context for backend diagnosis without exposing internal
                    // details to client
                    log.error("[HARDEN] Last-resort identity resolution failed for email={} sub={} | error={}",
                            principalDetails.getEmail(), jwt.getSubject(), e.getMessage());
                }
            }
        }

        if (userId == null || userId == -1L) {
            // [HARDEN] Graceful degradation: Return 503 (transient) not 500 (fatal).
            // This signals the client to retry rather than report a permanent failure.
            log.error("[HARDEN] Identity unresolvable for email={}. Returning 503 for client retry.",
                    principalDetails.getEmail());
            return ResponseEntity.status(503).body(
                    ApiResponse.<UserResponse>error("Profile temporarily unavailable. Please try again in a moment."));
        }

        UserResponse response = getUserUseCase.getUserById(userId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Timed(value = "user.me.update", description = "Time to update current user")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @Valid @RequestBody UserSelfUpdateRequest request,
            org.springframework.security.core.Authentication authentication) {

        Long userId = principalDetails.getId();

        if (userId == null || userId == -1L) {
            // Try resolution if credentials available
            if (authentication.getCredentials() instanceof Jwt jwt) {
                userId = identitySyncUseCase.syncUserFromKeycloak(
                        jwt.getSubject(),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("given_name"),
                        jwt.getClaimAsString("family_name"),
                        jwt.getClaimAsString("phone_number"),
                        jwt.getClaim("email_verified"));
            }
        }

        if (userId == null || userId == -1L) {
            return ResponseEntity.status(500).body(ApiResponse.<UserResponse>error("Identity resolution failure"));
        }

        log.info("User {} updating own profile", userId);

        UserResponse response = manageUserUseCase.updateSelf(userId, request);
        auditService.logUserAction(userId, userId, UserAction.SELF_UPDATE);

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    // ==================== USER CRUD ENDPOINTS ====================

    @GetMapping("/{id}")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin) or @userSecurity.isCurrentUser(#id)")
    @Timed(value = "user.get", description = "Time to get user by ID")
    @Operation(summary = "Get user by ID", description = "Users can view own profile, admins can view any")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "User ID") @PathVariable @Positive(message = "User ID must be positive") Long id,
            WebRequest request) {

        UserResponse response = getUserUseCase.getUserById(id);

        // ETag support
        String etag = generateETag(response);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(30, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    @Timed(value = "user.update", description = "Time to update user")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UserUpdateRequest request,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("User {} updating user {}", currentUser.getId(), id);

        UserResponse response = manageUserUseCase.updateUser(id, request);
        auditService.logUserAction(currentUser.getId(), id, UserAction.UPDATE);

        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @RateLimiter(name = "adminOperations")
    @Timed(value = "user.delete", description = "Time to delete user")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Prevent self-deletion
        if (id.equals(currentUser.getId())) {
            log.warn("Admin {} attempted self-deletion", currentUser.getId());
            throw new BusinessException("Cannot delete your own account", "USER_SELF_DELETE", HttpStatus.BAD_REQUEST);
        }

        log.info("Admin {} deleting user {} (hardDelete={})", currentUser.getId(), id, hardDelete);

        if (hardDelete) {
            manageUserUseCase.hardDeleteUser(id);
        } else {
            manageUserUseCase.softDeleteUser(id);
        }

        auditService.logUserAction(currentUser.getId(), id,
                hardDelete ? UserAction.HARD_DELETE : UserAction.SOFT_DELETE);

        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    // ==================== ADMIN LIST ENDPOINTS ====================

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "user.list", description = "Time to list users")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) Boolean active) {

        validateSortField(sortBy);

        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
                .orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        PageResponse<UserResponse> response = getUserUseCase.getAllUsers(pageable);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.success(response));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "user.byRole", description = "Time to get users by role")
    @Operation(summary = "Get users by role (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsersByRole(
            @PathVariable UserRole role, // Spring auto-validates enum
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        validateSortField(sortBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        PageResponse<UserResponse> response = getUserUseCase.getUsersByRole(role.name(), pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "searchApi")
    @Timed(value = "user.search", description = "Time to search users")
    @Operation(summary = "Search users (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        String sanitizedKeyword = sanitizeSearchKeyword(keyword);
        log.debug("Searching users with keyword: '{}'", sanitizedKeyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        PageResponse<UserResponse> response = getUserUseCase.searchUsers(sanitizedKeyword, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== ADMIN STATUS MANAGEMENT ====================

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "adminOperations")
    @Operation(summary = "Activate user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} activating user {}", currentUser.getId(), id);

        UserResponse response = manageUserUseCase.activateUser(id);
        auditService.logUserAction(currentUser.getId(), id, UserAction.ACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("User activated successfully", response));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "adminOperations")
    @Operation(summary = "Deactivate user account (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Prevent self-deactivation
        if (id.equals(currentUser.getId())) {
            log.warn("Admin {} attempted self-deactivation", currentUser.getId());
            throw new BusinessException("Cannot deactivate your own account", "USER_SELF_DEACTIVATE",
                    HttpStatus.BAD_REQUEST);
        }

        log.info("Admin {} deactivating user {}", currentUser.getId(), id);

        UserResponse response = manageUserUseCase.deactivateUser(id);
        auditService.logUserAction(currentUser.getId(), id, UserAction.DEACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", response));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "adminOperations")
    @Operation(summary = "Change user role (Admin only)")
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable @Positive Long id,
            @RequestBody @Valid RoleChangeRequest request,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Prevent changing own role
        if (id.equals(currentUser.getId())) {
            throw new BusinessException("Cannot change your own role", "USER_SELF_ROLE_CHANGE", HttpStatus.BAD_REQUEST);
        }

        log.info("Admin {} changing role of user {} to {}",
                currentUser.getId(), id, request.getNewRole());

        UserResponse response = manageUserUseCase.changeRole(id, request.getNewRole());
        auditService.logUserAction(currentUser.getId(), id, UserAction.ROLE_CHANGE);

        return ResponseEntity.ok(ApiResponse.success("User role changed successfully", response));
    }

    // ==================== BULK OPERATIONS ====================

    @PostMapping("/bulk/activate")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "bulkOperations")
    @Operation(summary = "Bulk activate users (Admin only)")
    public ResponseEntity<ApiResponse<BulkOperationResult>> bulkActivate(
            @RequestBody @Size(min = 1, max = 100, message = "Must provide 1-100 user IDs") List<@Positive Long> userIds,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} bulk activating {} users", currentUser.getId(), userIds.size());

        BulkOperationResult result = manageUserUseCase.bulkActivate(userIds);
        auditService.logBulkAction(currentUser.getId(), userIds, UserAction.BULK_ACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("Bulk activation completed", result));
    }

    @PostMapping("/bulk/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "bulkOperations")
    @Operation(summary = "Bulk deactivate users (Admin only)")
    public ResponseEntity<ApiResponse<BulkOperationResult>> bulkDeactivate(
            @RequestBody @Size(min = 1, max = 100) List<@Positive Long> userIds,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Prevent self-deactivation
        if (userIds.contains(currentUser.getId())) {
            throw new BusinessException("Cannot deactivate your own account", "USER_SELF_DEACTIVATE",
                    HttpStatus.BAD_REQUEST);
        }

        log.info("Admin {} bulk deactivating {} users", currentUser.getId(), userIds.size());

        BulkOperationResult result = manageUserUseCase.bulkDeactivate(userIds);
        auditService.logBulkAction(currentUser.getId(), userIds, UserAction.BULK_DEACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("Bulk deactivation completed", result));
    }

    // ==================== EXPORT ====================

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimiter(name = "exportApi")
    @Operation(summary = "Export users (Admin only)")
    public ResponseEntity<Resource> exportUsers(
            @RequestParam(defaultValue = "CSV") ExportFormat format,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean active,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} exporting users (format={}, role={}, active={})",
                currentUser.getId(), format, role, active);

        byte[] data = getUserUseCase.exportUsers(format, role, active);
        String filename = "users-export-" + LocalDate.now() + "." + format.getExtension();

        auditService.logUserAction(currentUser.getId(), null, UserAction.EXPORT);

        return ResponseEntity.ok()
                .contentType(format.getMediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(new ByteArrayResource(data));
    }

    // ==================== HELPER METHODS ====================

    private void validateSortField(String sortBy) {
        if (!ALLOWED_SORT_FIELDS.contains(sortBy.toLowerCase())) {
            throw new BusinessException(
                    "Invalid sort field '" + sortBy + "'. Allowed fields: " + ALLOWED_SORT_FIELDS,
                    "INVALID_SORT_FIELD",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String sanitizeSearchKeyword(String keyword) {
        return keyword.trim()
                .replaceAll("[%_\\[\\]\\\\]", "")
                .replaceAll("\\s+", " ")
                .replaceAll("[<>\"';]", "");
    }

    private String generateETag(UserResponse response) {
        return "\"user-" + response.getId() + "-" +
                (response.getUpdatedAt() != null ? response.getUpdatedAt().hashCode() : 0) + "\"";
    }
}




