package com.eshop.app.admin.api.controller;

import com.eshop.app.admin.api.request.AdminPasswordResetRequest;
import com.eshop.app.admin.api.request.AdminUserUpdateRequest;
import com.eshop.app.admin.application.service.KeycloakAdminService;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.user.api.request.RegisterRequest;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Keycloak Admin Controller
 *
 * <p><strong>SECURITY-CRITICAL:</strong> This controller directly mutates the platform's
 * identity provider (Keycloak) — account creation, deletion, password reset, and profile
 * update. It carries a strictly higher blast radius than domain-level admin operations
 * in {@code UserController} and is held to the same hardening standard: per-method
 * rate limiting, path/body validation, structured actor-attributed logging, timeouts on
 * every upstream call, and centralized error mapping (see
 * {@code GlobalExceptionHandler#handleKeycloakException} /
 * {@code #handleWebClientResponseException}).</p>
 *
 * <p>Note on {@code @PreAuthorize} + reactive ({@code Mono}) return types: this
 * application runs on the Spring MVC (servlet) stack — {@code @EnableMethodSecurity} in
 * {@code SecurityConfig}, not {@code @EnableReactiveMethodSecurity} — so the
 * pre-invocation authorization check executes synchronously against the servlet-thread
 * {@code SecurityContext} before the returned {@code Mono} pipeline is even
 * constructed. This is the same, standard enforcement mechanism used everywhere else in
 * this codebase; it is unaffected by the method's reactive return type.</p>
 *
 * <p><strong>Known audit-trail gap:</strong> every mutating method logs the acting
 * admin's ID, action, and target via structured {@code log.info}, but this is not yet
 * wired into a persisted, queryable audit store. {@code UserAuditService} (used by
 * {@code UserController}) cannot be reused as-is: its {@code logUserAction(Long, Long,
 * UserAction)} signature expects the platform's internal numeric user ID for both actor
 * and target, whereas the target here is Keycloak's external string UUID, and
 * {@code UserAuditService} is currently an unimplemented no-op stub for every caller
 * regardless. Wiring real persisted audit logging for identity-provider mutations
 * (ideally via a signature extension accepting a String target identifier) should block
 * production sign-off for this controller, consistent with the security review.</p>
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Admin - Identity Provider", description = "Keycloak identity provider administration endpoints")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole(@appProperties.security.roles.admin)")
public class KeycloakAdminController {

    // Keycloak IDs are always UUIDs in this deployment; validated as a defense-in-depth
    // format guard before the identifier is forwarded to the Admin REST API, and to keep
    // arbitrary/oversized input out of log statements.
    private static final String UUID_PATTERN =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration KEYCLOAK_CALL_TIMEOUT = Duration.ofSeconds(10);

    private final KeycloakAdminService adminService;

    /**
     * Create a new user directly in Keycloak (admin-initiated provisioning).
     *
     * <p>NOTE (architecture): reuses the self-registration {@code RegisterRequest} DTO,
     * which is already {@code @Valid}-annotated with the platform's real field
     * constraints (including password policy). It carries some self-registration-only
     * fields (e.g. {@code confirmPassword}) that are simply ignored by
     * {@code KeycloakAdminService#createUser}. A dedicated admin-creation DTO would be
     * cleaner but is a larger cross-module change; flagged as a follow-up rather than
     * done here to avoid an unrequested API-shape change beyond this review's scope.</p>
     */
    @PostMapping("/users")
    @RateLimiter(name = "adminOperations")
    @Timed(value = "admin.keycloak.user.create", description = "Time to create a Keycloak user")
    @Operation(summary = "Create a new Keycloak user (Admin only)")
    public Mono<ResponseEntity<Map<String, String>>> createUser(
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} creating Keycloak user with email={}", currentUser.getId(), maskEmail(request.getEmail()));

        return adminService.createUser(request)
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    /**
     * List Keycloak users, paginated via Keycloak's native offset pagination
     * ({@code first}/{@code max}), forwarded end-to-end by
     * {@code KeycloakAdminService#getAllUsers(int, int)}.
     */
    @GetMapping("/users")
    @Timed(value = "admin.keycloak.user.list", description = "Time to list Keycloak users")
    @Operation(summary = "List Keycloak users (Admin only, paginated)")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getAllUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} listing Keycloak users (page={}, size={})", currentUser.getId(), page, size);

        return adminService.getAllUsers(page, size)
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/users/{email}")
    @RateLimiter(name = "searchApi")
    @Timed(value = "admin.keycloak.user.getByEmail", description = "Time to fetch a Keycloak user by email")
    @Operation(summary = "Get Keycloak user by email (Admin only)")
    public Mono<ResponseEntity<Map<String, Object>>> getUserByEmail(
            @PathVariable @NotBlank @Email(message = "Must be a valid email address") String email,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} fetching Keycloak user by email={}", currentUser.getId(), maskEmail(email));

        return adminService.getUserByEmail(email)
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("/users/{userId}")
    @RateLimiter(name = "adminOperations")
    @Timed(value = "admin.keycloak.user.delete", description = "Time to delete a Keycloak user")
    @Operation(summary = "Delete Keycloak user (Admin only)")
    public Mono<ResponseEntity<Map<String, String>>> deleteUser(
            @PathVariable @Pattern(regexp = UUID_PATTERN, message = "Invalid user identifier format") String userId,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} deleting Keycloak user={}", currentUser.getId(), userId);

        return adminService.deleteUser(userId)
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    /**
     * Reset a user's password. The raw {@code Map<String,String>} body previously used
     * here accepted any/no "password" key with zero validation (a null or empty password
     * could be forwarded straight to Keycloak). {@link AdminPasswordResetRequest} enforces
     * the same password-strength policy used by self-service reset
     * ({@code ResetPasswordRequest}: 8-100 chars, upper/lower/digit/special character).
     */
    @PutMapping("/users/{userId}/reset-password")
    @RateLimiter(name = "adminOperations")
    @Timed(value = "admin.keycloak.user.resetPassword", description = "Time to reset a Keycloak user's password")
    @Operation(summary = "Reset a Keycloak user's password (Admin only)")
    public Mono<ResponseEntity<Map<String, String>>> resetPassword(
            @PathVariable @Pattern(regexp = UUID_PATTERN, message = "Invalid user identifier format") String userId,
            @Valid @RequestBody AdminPasswordResetRequest request,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} resetting password for Keycloak user={}", currentUser.getId(), userId);

        return adminService.resetPassword(userId, request.getPassword(), request.isTemporary())
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    /**
     * Update a user's profile fields. The raw {@code Map<String,Object>} body previously
     * used here was a mass-assignment vector (CWE-915): any Keycloak user-representation
     * field the caller supplied was forwarded verbatim to the identity provider with no
     * allow-list. {@link AdminUserUpdateRequest} exposes only the fields legitimately
     * safe for an admin to change via this endpoint; extend it deliberately as genuine
     * needs arise — never widen it back to an open map.
     */
    @PutMapping("/users/{userId}")
    @RateLimiter(name = "adminOperations")
    @Timed(value = "admin.keycloak.user.update", description = "Time to update a Keycloak user")
    @Operation(summary = "Update a Keycloak user's profile fields (Admin only)")
    public Mono<ResponseEntity<Map<String, String>>> updateUser(
            @PathVariable @Pattern(regexp = UUID_PATTERN, message = "Invalid user identifier format") String userId,
            @Valid @RequestBody AdminUserUpdateRequest updates,
            @AuthenticationPrincipal PrincipalDetails currentUser) {

        log.info("Admin {} updating Keycloak user={}", currentUser.getId(), userId);

        return adminService.updateUser(userId, updates)
                .timeout(KEYCLOAK_CALL_TIMEOUT)
                .map(ResponseEntity::ok);
    }

    /**
     * Defense-in-depth against log injection (CWE-117): strips CR/LF before logging an
     * externally-supplied email, and masks the local part so full addresses are not
     * written to logs verbatim.
     */
    private String maskEmail(String email) {
        if (email == null) {
            return "unknown";
        }
        String sanitized = email.replaceAll("[\\r\\n]", "");
        int at = sanitized.indexOf('@');
        return at > 1 ? sanitized.charAt(0) + "***" + sanitized.substring(at) : "***";
    }
}
