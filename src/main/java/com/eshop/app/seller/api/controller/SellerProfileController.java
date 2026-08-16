package com.eshop.app.seller.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.user.api.request.SellerProfileUpdateRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.application.port.in.SellerProfileUseCase;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller Profile Controller — Manages seller profile retrieval, updates, and existence checks.
 *
 * <p>Handles authenticated seller self-service profile operations:
 *
 * <ul>
 *   <li>Retrieve complete profile (KYC, bank accounts, documents, optional details)
 *   <li>Update profile sections (shop info, KYC, documents, bank, farmer, business)
 *   <li>Check registration completion status
 * </ul>
 *
 * <p>All endpoints require authentication. Profile updates require SELLER role. Profile reads and
 * existence checks are accessible to CUSTOMER, SELLER, and ADMIN roles to support onboarding flow
 * and admin review workflows.
 *
 * <p>Delegates exclusively to {@link SellerProfileUseCase} — no business logic in this class.
 */
@Tag(
        name = "Seller Profile",
        description =
                "Seller profile self-service — Retrieve, update, and check the registration"
                        + " status of the authenticated seller's profile. All endpoints require"
                        + " authentication.")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SellerProfileController {

    private final SellerProfileUseCase sellerProfileService;

    // =========================================================================
    // Profile Read Operations
    // =========================================================================

    /**
     * Retrieve the authenticated seller's complete profile.
     *
     * @param authentication the authenticated principal (injected by Spring Security)
     * @return the complete seller profile response
     */
    @GetMapping("/profile")
    @PreAuthorize(
            "hasAnyRole(@appProperties.security.roles.customer,"
                    + " @appProperties.security.roles.seller,"
                    + " @appProperties.security.roles.admin)")
    @Observed(name = "seller.profile.get", contextualName = "seller-get-profile")
    @Operation(
            summary = "Get seller profile",
            description =
                    """
                    Retrieve the authenticated seller's complete profile information.

                    **Returns:**
                    - Core profile (identity type, shop name, status, phone)
                    - KYC details (PAN, GSTIN, verification status)
                    - Documents (Aadhaar, PAN card, Business license — with verification status)
                    - Bank accounts (account number, IFSC, primary flag, verification status)
                    - Optional: Farmer details (farm location, land area, own-produce flag)
                    - Optional: Business details (legal name, signatory, warehouse location)
                    - Optional: Wholesale configuration (bulk pricing, minimum order)

                    **Authorization:** CUSTOMER, SELLER, or ADMIN role required.
                    CUSTOMER role is permitted to support the onboarding check flow.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile retrieved successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — CUSTOMER, SELLER, or ADMIN role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Not Found — No seller profile exists for the authenticated user")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getProfile(
            @Parameter(hidden = true) Authentication authentication) {

        log.debug("Seller profile retrieval requested — principal: {}", authentication.getName());
        SellerProfileResponse response = sellerProfileService.getSellerProfile(authentication);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Check whether the authenticated user has completed seller profile registration.
     *
     * <p>Returns {@code true} if a seller profile exists for the current user, {@code false} if
     * registration has not been completed. Used by the frontend onboarding flow to determine
     * whether to redirect to registration or the dashboard.
     *
     * @param authentication the authenticated principal (injected by Spring Security)
     * @return {@code true} if profile exists; {@code false} if registration required
     */
    @GetMapping("/profile/exists")
    @PreAuthorize(
            "hasAnyRole(@appProperties.security.roles.customer,"
                    + " @appProperties.security.roles.seller,"
                    + " @appProperties.security.roles.admin)")
    @Observed(name = "seller.profile.exists", contextualName = "seller-check-profile-exists")
    @Operation(
            summary = "Check if seller profile exists",
            description =
                    """
                    Check if the authenticated user has completed seller profile registration.

                    **Use Case:** Called by the frontend onboarding flow immediately after
                    login to determine whether to route the user to:
                    - `true` → Seller dashboard (profile exists)
                    - `false` → Seller registration flow (profile required)

                    **Authorization:** CUSTOMER, SELLER, or ADMIN role required.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile existence check completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — CUSTOMER, SELLER, or ADMIN role required")
    })
    public ResponseEntity<ApiResponse<Boolean>> checkProfileExists(
            @Parameter(hidden = true) Authentication authentication) {

        boolean exists = sellerProfileService.hasProfile(authentication);
        log.debug(
                "Seller profile existence check — principal: {}, exists: {}",
                authentication.getName(),
                exists);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    // =========================================================================
    // Profile Write Operations
    // =========================================================================

    /**
     * Update the authenticated seller's profile.
     *
     * <p>Supports partial updates — only provided fields are updated. The seller profile status
     * cannot be modified through this endpoint.
     *
     * @param request the update request containing fields to modify
     * @return the updated seller profile
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole(@appProperties.security.roles.seller)")
    @Observed(name = "seller.profile.update", contextualName = "seller-update-profile")
    @Operation(
            summary = "Update seller profile",
            description =
                    """
                    Update the authenticated seller's profile information.

                    **Updatable Sections:**
                    - Profile: shopName, businessName, description, phone
                    - KYC: panNumber, gstin, gstRegistered
                    - Documents: aadhar, pan, registrationProof (re-submission)
                    - Bank: accountNumber, ifscCode, bankName
                    - Farmer: farmLocationVillage, landArea, isOwnProduce
                    - Business: legalBusinessName, authorizedSignatory, warehouseLocation
                    - Wholesale: bulkPricingEnabled, minOrderQuantity

                    **Immutable Fields:** status, identityType, userId, createdAt.
                    Status transitions are admin-only operations.

                    **Authorization:** SELLER role required. CUSTOMER and ADMIN roles
                    cannot update seller profiles through this endpoint.
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Profile updated successfully",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request — Request body validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "Forbidden — SELLER role required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Not Found — No seller profile exists for the authenticated user"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Conflict — Shop name or handle already taken by another seller")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> updateProfile(
            @Valid @RequestBody SellerProfileUpdateRequest request) {

        Long userId = SecurityUtils.getAuthenticatedUserId();

        // Defensive guard: @PreAuthorize guarantees authentication, but SecurityUtils
        // reads from SecurityContextHolder which could theoretically be null in test
        // or proxy-misconfiguration scenarios. Fail-fast with a clear message
        // rather than propagating null to the database layer.
        if (userId == null) {
            log.error(
                    "SECURITY: updateProfile called with null userId — SecurityContext may be"
                            + " misconfigured. @PreAuthorize should prevent this. Investigate"
                            + " SecurityConfig and AOP proxy configuration.");
            throw new IllegalStateException(
                    "Cannot resolve authenticated user identity. Authentication context is"
                            + " absent.");
        }

        log.info("Seller profile update initiated — userId: {}", userId);
        SellerProfileResponse response = sellerProfileService.updateSellerProfile(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Seller profile updated successfully", response));
    }
}
