package com.eshop.app.user.api.controller;

import com.eshop.app.user.shared.domain.enums.ExportFormat;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.inventory.application.service.UserAuditService;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.BulkOperationResult;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.IS_ADMIN;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.IS_ADMIN_OR_SELF;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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

    // Canonical-case lookup: validated case-insensitively, but the mapped value is always
    // passed to Sort.by(...) in the exact case Hibernate/JPA expects for the entity property.
    private static final Map<String, String> ALLOWED_SORT_FIELDS = Map.of(
            "id", "id",
            "createdat", "createdAt",
            "firstname", "firstName",
            "lastname", "lastName",
            "role", "role");
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

        Long userId = resolveUserId(principalDetails, authentication);

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

        Long userId = resolveUserId(principalDetails, authentication);

        if (userId == null || userId == -1L) {
            log.error("[HARDEN] Identity unresolvable for email={}. Returning 503 for client retry.",
                    principalDetails.getEmail());
            return ResponseEntity.status(503).body(
                    ApiResponse.<UserResponse>error("Profile temporarily unavailable. Please try again in a moment."));
        }

        log.info("User {} updating own profile", userId);

        UserResponse response = manageUserUseCase.updateSelf(userId, request);
        auditService.logUserAction(userId, userId, UserAction.SELF_UPDATE);

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * Shared JIT identity-resolution fallback for /me endpoints. Used by both GET and PUT
     * so a Keycloak-authenticated principal missing its local user ID gets identical,
     * exception-safe resolution and identical 503-on-failure semantics either way.
     */
    private Long resolveUserId(PrincipalDetails principalDetails,
            org.springframework.security.core.Authentication authentication) {
        Long userId = principalDetails.getId();
        if (userId != null && userId != -1L) {
            return userId;
        }

        log.warn("Principal {} has missing local ID. Attempting last-resort resolution.",
                principalDetails.getEmail());

        // Resolved from PrincipalDetails (populated once, correctly, in
        // SecurityConfig#jwtAuthenticationConverter) — NOT authentication.getCredentials(),
        // which is always null here: ProviderManager erases credentials after
        // authentication succeeds, before this method ever runs. See
        // PrincipalDetails#getIssuer() javadoc. keycloakId is required for identity sync;
        // without it there's nothing to resolve against.
        if (principalDetails.getKeycloakId() == null || principalDetails.getKeycloakId().isBlank()) {
            return userId;
        }

        try {
            userId = identitySyncUseCase.syncUserFromKeycloak(
                    principalDetails.getKeycloakId(),
                    principalDetails.getEmail(),
                    principalDetails.getGivenName(),
                    principalDetails.getFamilyName(),
                    principalDetails.getPhoneNumber(),
                    principalDetails.getEmailVerified());
            log.info("[HARDEN] Resolved local identity for user {} as ID: {}", principalDetails.getEmail(),
                    userId);
        } catch (Exception e) {
            // [HARDEN] Log full context for backend diagnosis without exposing internal
            // details to client
            log.error("[HARDEN] Last-resort identity resolution failed for email={} sub={} | error={}",
                    principalDetails.getEmail(), principalDetails.getKeycloakId(), e.getMessage());
            return null;
        }
        return userId;
    }

    // ==================== USER CRUD ENDPOINTS ====================

    @GetMapping("/{id}")
    @PreAuthorize(IS_ADMIN_OR_SELF)
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

    // Admin-only: UserUpdateRequest exposes fields (e.g. email) that the self-service
    // UserSelfUpdateRequest deliberately omits. Self-updates go through PUT /me instead,
    // which uses the restricted DTO/use case — do not grant self-access here.
    @PutMapping("/{id}")
    @PreAuthorize(IS_ADMIN)
    @Timed(value = "user.update", description = "Time to update user")
    @Operation(summary = "Update user profile (Admin only — use PUT /me for self-service)")
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
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "adminOperations")
    @Timed(value = "user.delete", description = "Time to delete user")
    @Operation(summary = "Delete user (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - admins cannot delete their own account")
    })
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Self-deletion is blocked by UserSelfProtectionGuard inside the service layer.
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
    @PreAuthorize(IS_ADMIN)
    @Timed(value = "user.list", description = "Time to list users")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection,
            @RequestParam(required = false) Boolean active) {

        String canonicalSortBy = validateAndNormalizeSortField(sortBy);

        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
                .orElse(Sort.Direction.ASC);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, canonicalSortBy));

        PageResponse<UserResponse> response = active != null
                ? getUserUseCase.getUsersByActiveStatus(active, pageable)
                : getUserUseCase.getAllUsers(pageable);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(10, TimeUnit.SECONDS).cachePrivate())
                .body(ApiResponse.success(response));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize(IS_ADMIN)
    @Timed(value = "user.byRole", description = "Time to get users by role")
    @Operation(summary = "Get users by role (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsersByRole(
            @PathVariable UserRole role, // Spring auto-validates enum
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        String canonicalSortBy = validateAndNormalizeSortField(sortBy);
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
                .orElse(Sort.Direction.ASC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, canonicalSortBy));
        PageResponse<UserResponse> response = getUserUseCase.getUsersByRole(role.name(), pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "searchApi")
    @Timed(value = "user.search", description = "Time to search users")
    @Operation(summary = "Search users (Admin only)")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> searchUsers(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size) {

        // Sanitization (SQL wildcard escaping) is already performed by SearchUtils.sanitize()
        // inside UserQueryService#searchUsers against a parameterized JPQL query — no need
        // to duplicate/destructively re-sanitize here (previously stripped valid characters
        // like apostrophes, corrupting names such as "O'Brien").
        String trimmedKeyword = keyword.trim();
        log.debug("Searching users with keyword: '{}'", trimmedKeyword);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id"));
        PageResponse<UserResponse> response = getUserUseCase.searchUsers(trimmedKeyword, pageable);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== ADMIN STATUS MANAGEMENT ====================

    @PutMapping("/{id}/activate")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "adminOperations")
    @Timed(value = "user.activate", description = "Time to activate user")
    @Operation(summary = "Activate user account (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - admins cannot activate their own account")
    })
    public ResponseEntity<ApiResponse<UserResponse>> activateUser(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} activating user {}", currentUser.getId(), id);

        UserResponse response = manageUserUseCase.activateUser(id);
        auditService.logUserAction(currentUser.getId(), id, UserAction.ACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("User activated successfully", response));
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "adminOperations")
    @Timed(value = "user.deactivate", description = "Time to deactivate user")
    @Operation(summary = "Deactivate user account (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - admins cannot deactivate their own account")
    })
    public ResponseEntity<ApiResponse<UserResponse>> deactivateUser(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Self-deactivation is blocked by UserSelfProtectionGuard inside the service layer.
        log.info("Admin {} deactivating user {}", currentUser.getId(), id);

        UserResponse response = manageUserUseCase.deactivateUser(id);
        auditService.logUserAction(currentUser.getId(), id, UserAction.DEACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("User deactivated successfully", response));
    }

    @PutMapping("/{id}/role")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "adminOperations")
    @Timed(value = "user.role.change", description = "Time to change user role")
    @Operation(summary = "Change user role (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - admins cannot change their own role")
    })
    public ResponseEntity<ApiResponse<UserResponse>> changeUserRole(
            @PathVariable @Positive Long id,
            @RequestBody @Valid RoleChangeRequest request,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Self-role-change is blocked by UserSelfProtectionGuard inside the service layer.
        log.info("Admin {} changing role of user {} to {}",
                currentUser.getId(), id, request.getNewRole());

        UserResponse response = manageUserUseCase.changeRole(id, request.getNewRole());
        auditService.logUserAction(currentUser.getId(), id, UserAction.ROLE_CHANGE);

        return ResponseEntity.ok(ApiResponse.success("User role changed successfully", response));
    }

    // ==================== BULK OPERATIONS ====================

    @PostMapping("/bulk/activate")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "bulkOperations")
    @Timed(value = "user.bulk.activate", description = "Time to bulk activate users")
    @Operation(summary = "Bulk activate users (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - the ID list includes the requesting admin's own account")
    })
    public ResponseEntity<ApiResponse<BulkOperationResult>> bulkActivate(
            @RequestBody @Size(min = 1, max = 100, message = "Must provide 1-100 user IDs") List<@Positive Long> userIds,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} bulk activating {} users", currentUser.getId(), userIds.size());

        BulkOperationResult result = manageUserUseCase.bulkActivate(userIds);
        auditService.logBulkAction(currentUser.getId(), userIds, UserAction.BULK_ACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("Bulk activation completed", result));
    }

    @PostMapping("/bulk/deactivate")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "bulkOperations")
    @Timed(value = "user.bulk.deactivate", description = "Time to bulk deactivate users")
    @Operation(summary = "Bulk deactivate users (Admin only)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Forbidden - the ID list includes the requesting admin's own account")
    })
    public ResponseEntity<ApiResponse<BulkOperationResult>> bulkDeactivate(
            @RequestBody @Size(min = 1, max = 100) List<@Positive Long> userIds,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        // Bulk self-deactivation is blocked by UserSelfProtectionGuard inside the service layer.
        log.info("Admin {} bulk deactivating {} users", currentUser.getId(), userIds.size());

        BulkOperationResult result = manageUserUseCase.bulkDeactivate(userIds);
        auditService.logBulkAction(currentUser.getId(), userIds, UserAction.BULK_DEACTIVATE);

        return ResponseEntity.ok(ApiResponse.success("Bulk deactivation completed", result));
    }

    // ==================== EXPORT ====================

    @GetMapping("/export")
    @PreAuthorize(IS_ADMIN)
    @RateLimiter(name = "exportApi")
    @Timed(value = "user.export", description = "Time to export users")
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

    // Validates sortBy case-insensitively but returns the CANONICAL, correctly-cased field
    // name that Sort.by(...) must actually use — the previous implementation validated
    // case-insensitively yet passed the caller's raw casing to Hibernate/JPA, so an
    // accepted value like "CREATEDAT" would still fail downstream with an unhandled
    // PropertyReferenceException.
    private String validateAndNormalizeSortField(String sortBy) {
        String canonical = ALLOWED_SORT_FIELDS.get(sortBy == null ? "" : sortBy.toLowerCase());
        if (canonical == null) {
            throw new BusinessException(
                    "Invalid sort field '" + sortBy + "'. Allowed fields: " + ALLOWED_SORT_FIELDS.values(),
                    "INVALID_SORT_FIELD",
                    HttpStatus.BAD_REQUEST);
        }
        return canonical;
    }

    private String generateETag(UserResponse response) {
        return "\"user-" + response.getId() + "-" +
                (response.getUpdatedAt() != null ? response.getUpdatedAt().hashCode() : 0) + "\"";
    }
}




