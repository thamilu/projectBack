package com.eshop.app.seller.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;
import com.eshop.app.seller.api.request.RejectionRequest;
import com.eshop.app.seller.application.port.in.SellerApprovalUseCase;
import com.eshop.app.user.api.response.SellerProfileResponse;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Seller Admin Controller — Manages administrative operations on seller profiles.
 *
 * <p>Exclusively handles privileged admin workflows:
 *
 * <ul>
 *   <li>Retrieving pending seller registration requests
 *   <li>Approving pending seller profiles
 *   <li>Rejecting pending seller profiles with a documented reason
 * </ul>
 *
 * <p>All endpoints require ADMIN role. All state-changing actions are audit-logged.
 */
@Tag(
        name = "Seller Administration",
        description =
                "Admin-only seller management — Review, approve, and reject pending seller"
                        + " registration requests. All operations require ADMIN role.")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SellerAdminController {

    private static final String SYSTEM_PRINCIPAL = "system";

    private final SellerApprovalUseCase sellerAdminService;

    // =========================================================================
    // Admin Read Operations
    // =========================================================================

    /**
     * Retrieve all pending seller registration requests awaiting admin review.
     *
     * <p><strong>Authorization:</strong> ADMIN role required.
     */
    @GetMapping("/requests")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Observed(name = "seller.admin.requests", contextualName = "admin-get-pending-seller-requests")
    @Operation(
            summary = "Get pending seller requests",
            description =
                    """
                    Retrieve all pending seller registration requests awaiting admin review.

                    **Authorization:** Requires ADMIN role.

                    **Returns:** Paginated list of seller profiles in PENDING status, ordered by
                    registration date (oldest first for FIFO review).
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Pending requests retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<PageResponse<SellerProfileResponse>>> getPendingRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC)
                    Pageable pageable) {
        PageResponse<SellerProfileResponse> requests =
                sellerAdminService.getPendingSellers(pageable);
        log.debug(
                "Admin retrieved pending seller requests — page: {}, size: {}, total: {}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                requests.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // =========================================================================
    // Admin State-Changing Operations
    // =========================================================================

    /**
     * Approve a pending seller registration request.
     *
     * <p>Transitions the seller profile from {@code PENDING} to {@code ACTIVE} status and triggers
     * Keycloak role assignment via the service layer.
     *
     * <p><strong>Authorization:</strong> ADMIN role required.
     *
     * @param id the seller profile ID to approve
     * @param authentication the authenticated admin principal
     */
    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Observed(name = "seller.admin.approve", contextualName = "admin-approve-seller-request")
    @Operation(
            summary = "Approve seller request",
            description =
                    """
                    Approve a pending seller registration request.

                    **Authorization:** Requires ADMIN role.

                    **Effect:**
                    - Seller profile status transitions: PENDING → ACTIVE
                    - SELLER role assigned in Keycloak
                    - Approval notification dispatched (if configured)
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Seller approved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request — Seller is not in PENDING status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Seller profile not found")
    })
    public ResponseEntity<ApiResponse<Void>> approveSeller(
            @Parameter(
                            description = "Seller profile ID to approve",
                            required = true,
                            example = "42")
                    @PathVariable
                    @Min(1)
                    Long id,
            @Parameter(hidden = true) Authentication authentication) {

        String adminName = resolveAdminName(authentication);
        log.info("Admin '{}' approving seller profile — profileId: {}", adminName, id);

        sellerAdminService.approveSeller(id, adminName);

        return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", null));
    }

    /**
     * Reject a pending seller registration request with a documented reason.
     *
     * <p>Transitions the seller profile from {@code PENDING} to {@code REJECTED} status. The
     * rejection reason is persisted and communicated to the seller.
     *
     * <p><strong>Authorization:</strong> ADMIN role required.
     *
     * @param id the seller profile ID to reject
     * @param request the rejection request containing the mandatory reason
     * @param authentication the authenticated admin principal
     */
    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Observed(name = "seller.admin.reject", contextualName = "admin-reject-seller-request")
    @Operation(
            summary = "Reject seller request",
            description =
                    """
                    Reject a pending seller registration request with a documented reason.

                    **Authorization:** Requires ADMIN role.

                    **Effect:**
                    - Seller profile status transitions: PENDING → REJECTED
                    - Rejection reason is persisted on the profile
                    - Rejection notification dispatched (if configured)

                    **Rejection reason** is mandatory and must be meaningful — it is
                    communicated to the seller to guide resubmission.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Seller rejected successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description =
                        "Bad Request — Rejection reason is required or seller is not in PENDING"
                                + " status"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Seller profile not found")
    })
    public ResponseEntity<ApiResponse<Void>> rejectSeller(
            @Parameter(description = "Seller profile ID to reject", required = true, example = "42")
                    @PathVariable
                    @Min(1)
                    Long id,
            @Valid @RequestBody RejectionRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        String adminName = resolveAdminName(authentication);
        log.info("Admin '{}' rejecting seller profile — profileId: {}", adminName, id);

        sellerAdminService.rejectSeller(id, request.getReason(), adminName);

        return ResponseEntity.ok(ApiResponse.success("Seller rejected successfully", null));
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Resolves the admin principal name from the {@link Authentication} object.
     *
     * <p>Returns {@value SYSTEM_PRINCIPAL} as a safe fallback if the authentication context is
     * unexpectedly absent — ensuring audit log continuity without throwing a {@link
     * NullPointerException} from the controller layer.
     *
     * <p><strong>Note:</strong> {@code @PreAuthorize} guarantees a non-null {@link Authentication}
     * under normal Spring Security operation. The null fallback protects against misconfigured
     * proxy or test contexts.
     *
     * @param authentication the Spring Security authentication object
     * @return the principal name or {@value SYSTEM_PRINCIPAL}
     */
    private String resolveAdminName(Authentication authentication) {
        if (authentication == null) {
            log.warn(
                    "Authentication context is absent on an admin endpoint — defaulting to '{}'."
                            + " Investigate SecurityConfig or proxy configuration.",
                    SYSTEM_PRINCIPAL);
            return SYSTEM_PRINCIPAL;
        }
        if (authentication.getPrincipal() instanceof PrincipalDetails principalDetails) {
            if (principalDetails.getEmail() != null) {
                return principalDetails.getEmail();
            }
            if (principalDetails.getKeycloakId() != null) {
                return principalDetails.getKeycloakId();
            }
        }
        if (authentication.getName() == null) {
            log.warn(
                    "Authentication context is absent on an admin endpoint — defaulting to '{}'."
                            + " Investigate SecurityConfig or proxy configuration.",
                    SYSTEM_PRINCIPAL);
            return SYSTEM_PRINCIPAL;
        }
        return authentication.getName();
    }
}
