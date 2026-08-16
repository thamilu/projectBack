package com.eshop.app.seller.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.util.SecurityUtils;
import com.eshop.app.seller.api.request.SellerRegisterRequest;
import com.eshop.app.seller.application.port.in.RegisterSellerUseCase;
import com.eshop.app.user.api.response.SellerProfileResponse;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Seller Registration Controller — Manages new seller profile registration.
 *
 * <p>Handles the single-step seller onboarding registration endpoint. The authenticated user
 * submits a complete registration payload in one request. The service layer validates the payload,
 * persists the seller profile with {@code PENDING} status, and triggers the Keycloak role
 * assignment workflow.
 *
 * <p><strong>Authorization model:</strong> This endpoint uses
 * {@code @PreAuthorize("isAuthenticated()")} rather than {@code hasRole('SELLER')} to avoid a
 * chicken-and-egg problem: a user cannot have the SELLER role before completing registration. Any
 * authenticated user (including CUSTOMER role) may submit a registration request. The service layer
 * enforces business rules (e.g., one profile per user, accepted terms required).
 *
 * <p><strong>Security note:</strong> PAN numbers, Aadhaar numbers, and bank account details present
 * in the request body are intentionally excluded from log statements to comply with PCI-DSS, GDPR,
 * and India's DPDP Act 2023.
 *
 * <p>Delegates exclusively to {@link RegisterSellerUseCase} — no business logic in this class.
 */
@Tag(
        name = "Seller Registration",
        description =
                "Seller onboarding registration — Submit a new seller profile for review."
                        + " Requires authentication. The seller profile is created with PENDING"
                        + " status and transitions to ACTIVE upon admin approval.")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SellerRegistrationController {

    private final RegisterSellerUseCase sellerRegistrationService;

    // =========================================================================
    // Seller Registration
    // =========================================================================

    /**
     * Register a new seller profile for the authenticated user.
     *
     * <p>Accepts a complete registration payload in a single request. The frontend may present this
     * as a multi-step wizard, but the API contract is a single {@code POST} with all fields.
     *
     * <p><strong>Authorization:</strong> Any authenticated user may register as a seller.
     * {@code @PreAuthorize("isAuthenticated()")} is used intentionally — the SELLER role is granted
     * after successful registration, not before.
     *
     * <p><strong>Idempotency:</strong> Not idempotent — submitting a second registration for a user
     * who already has a profile returns {@code 409 Conflict}.
     *
     * @param request the seller registration request containing identity, KYC, bank, and optional
     *     farmer/business details
     * @return the created seller profile with {@code PENDING} status and {@code 201 Created}
     */
    @PostMapping("/register")
    // INTENTIONAL: isAuthenticated() is used instead of hasRole('SELLER') to avoid the
    // chicken-and-egg problem where a user cannot hold the SELLER role before registering.
    // The SecurityConfig filter chain also enforces .authenticated() for this path.
    // Business-rule enforcement (one profile per user, accepted terms) is in the service layer.
    @PreAuthorize("isAuthenticated()")
    @Observed(name = "seller.registration.register", contextualName = "seller-register-new-seller")
    @Operation(
            summary = "Register seller profile",
            description =
                    """
                    Submit a new seller registration request for the authenticated user.

                    **Single-payload API:** All registration data is submitted in one request.
                    The frontend may present this as a wizard, but the API accepts a
                    complete payload. All required fields must be provided in one call.

                    **Required Fields (all types):**
                    - `identityType` — INDIVIDUAL or BUSINESS
                    - `businessTypes` — one or more of FARMER, RETAILER, WHOLESALER, etc.
                    - `shopName` — display name for the seller's shop
                    - `phone` — seller contact number
                    - `acceptedTerms` — must be `true`
                    - `panNumber` — mandatory for all seller types
                    - `accountNumber` + `ifscCode` — primary bank account

                    **Conditional Fields:**
                    - `gstin` — required when `identityType = BUSINESS`
                    - `farmLocationVillage`, `landArea`, `isOwnProduce` — required for FARMER type

                    **Post-Registration:**
                    Profile is created with `PENDING` status. Admin review is required
                    before the SELLER role is assigned and the profile becomes `ACTIVE`.

                    **Authorization:** Any authenticated user. SELLER role is not required
                    (it is granted upon approval, not before registration).
                    """,
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Seller profile created successfully — awaiting admin review",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = ApiResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "Registration Success",
                                                value =
                                                        """
                                                                {
                                                                  "status": "success",
                                                                  "message": "Seller profile registered successfully",
                                                                  "data": {
                                                                    "id": 1,
                                                                    "userId": 123,
                                                                    "identityType": "INDIVIDUAL",
                                                                    "businessTypes": ["FARMER"],
                                                                    "shopName": "Green Valley Farm",
                                                                    "phone": "+919876543210",
                                                                    "status": "PENDING",
                                                                    "createdAt": "2026-01-11T10:30:00Z"
                                                                  }
                                                                }
                                                                """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description =
                        "Bad Request — Request validation failed"
                                + " (missing required fields, invalid format, terms not accepted)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Unauthorized — Valid Bearer token required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description =
                        "Conflict — A seller profile already exists for this user."
                                + " Each user may have at most one seller profile.")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> registerSeller(
            @Parameter(
                            description =
                                    "Complete seller registration payload. All required fields"
                                            + " must be provided. See schema for field-level"
                                            + " validation rules.",
                            required = true,
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    SellerRegisterRequest.class),
                                            examples = {
                                                @ExampleObject(
                                                        name = "Farmer Seller (INDIVIDUAL)",
                                                        value =
                                                                """
                                                                        {
                                                                          "identityType": "INDIVIDUAL",
                                                                          "businessTypes": ["FARMER"],
                                                                          "shopName": "Green Valley Farm",
                                                                          "phone": "+919876543210",
                                                                          "acceptedTerms": true,
                                                                          "panNumber": "ABCDE1234F",
                                                                          "farmLocationVillage": "Pune",
                                                                          "landArea": "10 acres",
                                                                          "isOwnProduce": true,
                                                                          "accountNumber": "12345678901234",
                                                                          "ifscCode": "SBIN0001234"
                                                                        }
                                                                        """),
                                                @ExampleObject(
                                                        name = "Business Seller (BUSINESS)",
                                                        value =
                                                                """
                                                                        {
                                                                          "identityType": "BUSINESS",
                                                                          "businessTypes": ["RETAILER"],
                                                                          "shopName": "TechHub Electronics",
                                                                          "phone": "+911234567890",
                                                                          "acceptedTerms": true,
                                                                          "panNumber": "FGHIJ5678K",
                                                                          "gstin": "29FGHIJ5678K1Z5",
                                                                          "gstRegistered": true,
                                                                          "accountNumber": "98765432109876",
                                                                          "ifscCode": "HDFC0001234"
                                                                        }
                                                                        """)
                                            }))
                    @Valid
                    @RequestBody
                    SellerRegisterRequest request) {

        Long userId = SecurityUtils.getAuthenticatedUserId();

        // Defensive null guard: @PreAuthorize("isAuthenticated()") guarantees a non-null
        // Authentication in the SecurityContext under normal Spring Security operation.
        // This guard protects against misconfigured AOP proxy, test context without
        // SecurityContextHolder population, or SecurityConfig bypass scenarios.
        // Fail-fast here prevents null propagating to the database layer (NOT NULL violation
        // on user_id column) which would produce a confusing 500 instead of a clear security error.
        if (userId == null) {
            log.error(
                    "SECURITY: registerSeller called with null userId — SecurityContext is absent."
                            + " @PreAuthorize(isAuthenticated()) should prevent this."
                            + " Investigate SecurityConfig and AOP proxy configuration.");
            throw new IllegalStateException(
                    "Cannot resolve authenticated user identity."
                            + " Authentication context is absent.");
        }

        log.info(
                "Seller registration initiated — userId: {}, identityType: {}",
                userId,
                request.getIdentityType());
        // NOTE: PAN, Aadhaar, GSTIN, and account numbers are intentionally excluded
        // from logs to comply with PCI-DSS, GDPR, and India's DPDP Act 2023.

        SellerProfileResponse response = sellerRegistrationService.registerSeller(userId, request);

        log.info(
                "Seller registration completed — userId: {}, profileId: {}, status: {}",
                userId,
                response.getId(),
                response.getStatus());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller profile registered successfully", response));
    }
}
