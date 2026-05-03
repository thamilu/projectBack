package com.eshop.app.controller;

import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.SellerProfileUpdateRequest;
import com.eshop.app.dto.request.SellerRegisterRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.service.SellerService;
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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Unified Seller Controller - Manages seller profiles for all seller types.
 * 
 * <p>
 * Supports step-wise seller onboarding with modular architecture:
 * SellerProfile → SellerKYC, SellerDocument, SellerBankAccount,
 * SellerFarmerDetails, SellerBusinessDetails, SellerWholesaleConfig
 * </p>
 */
@Tag(name = "Sellers", description = "Seller profile management - Register, view, and update seller profiles")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Slf4j
public class SellerController {

    private final SellerService sellerService;
    private final com.eshop.app.service.UserService userService;

    /**
     * Register a new seller profile.
     */
    @PostMapping("/register")
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.customer, @appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @Operation(summary = "Register seller profile", description = """
            Create a new seller profile for the authenticated user.

            **Step-wise Onboarding:**
            - Step 1 (Basic): identityType, shopName
            - Step 2 (KYC): panNumber (mandatory), gstin (conditional)
            - Step 3 (Bank): accountNumber, ifscCode
            - Step 4 (Optional): farmerDetails, businessDetails

            **Identity Types:** INDIVIDUAL, BUSINESS

            **Smart Validation:**
            - BUSINESS → GSTIN required
            - INDIVIDUAL → PAN only
                        """, security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Seller profile created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(name = "Success", value = """
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
                "kyc": { "panNumber": "ABCDE1234F", "verificationStatus": "PENDING" },
                "farmerDetails": { "farmLocation": "Pune", "landArea": "10 acres", "isOwnProduce": true },
                "bankAccounts": [{ "accountNumber": "12345678901234", "ifscCode": "SBIN0001234", "isPrimary": true }],
                "documents": [{ "documentType": "PAN", "verificationStatus": "PENDING" }],
                "createdAt": "2026-01-11T10:30:00Z"
              }
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - User already has a profile or validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - SELLER role required")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> registerSeller(
        @Parameter(description = "Seller registration request", required = true, content = @Content(schema = @Schema(implementation = SellerRegisterRequest.class), examples = {
            @ExampleObject(name = "Farmer Seller", value = """
                {
                  "identityType": "INDIVIDUAL",
                  "businessTypes": ["FARMER"],
                  "shopName": "Green Valley Farm",
                  "businessName": "Green Valley Organic Farms Pvt Ltd",
                  "phone": "+919876543210",
                  "description": "Organic vegetables and fruits",
                  "acceptedTerms": true,
                  "panNumber": "ABCDE1234F",
                  "aadhar": "123456789012",
                  "farmLocationVillage": "Pune",
                  "landArea": "10 acres",
                  "isOwnProduce": true,
                  "accountNumber": "12345678901234",
                  "ifscCode": "SBIN0001234"
                }
                """),
            @ExampleObject(name = "Business Seller", value = """
                {
                  "identityType": "BUSINESS",
                  "businessTypes": ["RETAILER"],
                  "shopName": "TechHub Electronics",
                  "businessName": "TechHub Electronics Pvt Ltd",
                  "phone": "+911234567890",
                  "description": "Electronics and gadgets retailer",
                  "acceptedTerms": true,
                  "panNumber": "FGHIJ5678K",
                  "gstin": "29FGHIJ5678K1Z5",
                  "gstRegistered": true,
                  "kycBusinessType": "PVT_LTD",
                  "legalBusinessName": "TechHub Electronics Pvt Ltd",
                  "authorizedSignatory": "Rahul Sharma",
                  "warehouseLocation": "Plot 12, Industrial Area, Bangalore",
                  "accountNumber": "98765432109876",
                  "ifscCode": "HDFC0001234"
                }
                """)
        })) @Valid @RequestBody SellerRegisterRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        Long userId = sellerService.resolveUserId(authentication);
        log.info("Seller registration request for userId: {}, identityType: {}", userId, request.getIdentityType());

        SellerProfileResponse response = sellerService.registerSeller(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Seller profile registered successfully", response));
    }

    /**
     * Get authenticated seller's profile.
     */
    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.customer, @appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @Operation(summary = "Get seller profile", description = """
            Retrieve the authenticated seller's complete profile information.

            **Returns:**
            - Core profile (identity, display name, status)
            - KYC details (PAN, GSTIN, verification status)
            - Documents (Aadhaar, PAN, License with verification)
            - Bank accounts (with primary flag)
            - Optional: Farmer details, Business details, Wholesale config
                        """, security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
            {
              "status": "success",
              "data": {
                "id": 1,
                "userId": 123,
                "identityType": "INDIVIDUAL",
                "businessTypes": ["FARMER"],
                "shopName": "Green Valley Farm",
                "phone": "+919876543210",
                "status": "ACTIVE",
                "kyc": { "panNumber": "ABCDE1234F", "verificationStatus": "VERIFIED" },
                "farmerDetails": { "isOwnProduce": true, "farmLocation": "Pune", "landArea": "10 acres" },
                "bankAccounts": [{ "accountNumber": "12345678901234", "isPrimary": true, "verificationStatus": "PENDING" }],
                "documents": [{ "documentType": "PAN", "documentNumber": "ABCDE1234F", "verificationStatus": "VERIFIED" }],
                "createdAt": "2026-01-10T10:30:00Z"
              }
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getProfile(
            @Parameter(hidden = true) Authentication authentication) {

      log.info("DEBUG: getProfile called. Auth: {}",
          authentication != null ? authentication.getClass().getName() : "null");
      try {
        SellerProfileResponse response = sellerService.getSellerProfile(authentication);
        return ResponseEntity.ok(ApiResponse.success(response));
      } catch (Exception e) {
        log.error("DEBUG: Exception in getProfile: " + e.getClass().getName() + " - " + e.getMessage(), e);
        throw e;
      }
    }

    /**
     * Update authenticated seller's profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("hasRole(@appProperties.security.roles.seller)")
    @Operation(summary = "Update seller profile", description = """
            Update the authenticated seller's profile information.

            **Updatable Sections:**
            - Profile: shopName, businessName, description
            - KYC: panNumber, gstin, gstRegistered
            - Documents: aadhar, pan, registrationProof
            - Bank: accountNumber, ifscCode, bankName
            - Farmer: farmLocationVillage, landArea, isOwnProduce
            - Business: legalBusinessName, authorizedSignatory, warehouseLocation
            - Wholesale: bulkPricingEnabled, minOrderQuantity

            **Note:** Status cannot be updated by the seller.
                        """, security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class), examples = @ExampleObject(value = """
            {
              "status": "success",
              "message": "Seller profile updated successfully",
              "data": {
                "id": 1,
                "identityType": "INDIVIDUAL",
                "shopName": "Green Valley Organic Farm",
                "status": "ACTIVE",
                "kyc": { "panNumber": "ABCDE1234F", "verificationStatus": "VERIFIED" },
                "farmerDetails": { "farmLocation": "Pune", "landArea": "12 acres" }
              }
            }
            """))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request - Validation failed"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Profile not found")
    })
    public ResponseEntity<ApiResponse<SellerProfileResponse>> updateProfile(
        @Parameter(description = "Seller profile update request", required = true, content = @Content(schema = @Schema(implementation = SellerProfileUpdateRequest.class), examples = @ExampleObject(value = """
            {
              "shopName": "Green Valley Organic Farm",
              "description": "Premium organic vegetables, fruits, and dairy products",
              "panNumber": "ABCDE1234F",
              "farmLocationVillage": "Pune Rural",
              "landArea": "12 acres"
            }
            """))) @Valid @RequestBody SellerProfileUpdateRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        Long userId = sellerService.resolveUserId(authentication);
        log.info("Updating seller profile for userId: {}", userId);

        SellerProfileResponse response = sellerService.updateSellerProfile(userId, request);

        return ResponseEntity.ok(ApiResponse.success("Seller profile updated successfully", response));
    }

    /**
     * Check if authenticated user has a seller profile.
     */
    @GetMapping("/profile/exists")
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.customer, @appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @Operation(summary = "Check if seller profile exists", description = """
            Check if the authenticated user has completed seller profile registration.

            **Returns:**
            - `true`: Profile exists
            - `false`: Registration required
                        """, security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Check completed successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiResponse<Boolean>> checkProfileExists(
            @Parameter(hidden = true) Authentication authentication) {

        boolean exists = sellerService.hasProfile(authentication);
        log.info("DEBUG: checkProfileExists returned: {}", exists);

        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    // --- Admin Endpoints ---

    /**
     * Get all pending seller requests (Admin only).
     */
    @GetMapping("/requests")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(summary = "Get pending seller requests", description = "Retrieve all pending seller registration requests. Requires ADMIN role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending requests retrieved successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    })
    public ResponseEntity<ApiResponse<java.util.List<SellerProfileResponse>>> getPendingRequests() {
      java.util.List<SellerProfileResponse> requests = sellerService.getPendingSellers();
      return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * Approve a seller request (Admin only).
     */
    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve seller request", description = "Approve a pending seller registration request. Requires ADMIN role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller approved successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller profile not found")
    })
    public ResponseEntity<ApiResponse<Void>> approveSeller(
        @PathVariable Long id,
        @Parameter(hidden = true) Authentication authentication) {

      String adminName = authentication != null ? authentication.getName() : "system";
      sellerService.approveSeller(id, adminName);
      return ResponseEntity.ok(ApiResponse.success("Seller approved successfully", null));
    }

    /**
     * Reject a seller request (Admin only).
     */
    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject seller request", description = "Reject a pending seller registration request with a reason. Requires ADMIN role.", security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seller rejected successfully", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Seller profile not found")
    })
    public ResponseEntity<ApiResponse<Void>> rejectSeller(
        @PathVariable Long id,
        @Valid @RequestBody com.eshop.app.dto.request.RejectionRequest request,
        @Parameter(hidden = true) Authentication authentication) {

      String adminName = authentication != null ? authentication.getName() : "system";
      sellerService.rejectSeller(id, request.getReason(), adminName);
      return ResponseEntity.ok(ApiResponse.success("Seller rejected successfully", null));
    }

    @PostMapping("/{id}/sync-role")
    @Operation(summary = "Debug: Sync Seller Role", description = "Manually trigger Keycloak role assignment for debugging.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Void>> syncSellerRole(@PathVariable Long id) {
      log.info("DEBUG: Endpoint /sync-role called for id: {}", id);
      sellerService.syncSellerRole(id);
      return ResponseEntity.ok(ApiResponse.success("Role sync attempted. Check logs.", null));
    }

    @PostMapping("/{id}/sync-keycloak-id/{keycloakId}")
    @Operation(summary = "Debug: Sync Keycloak ID", description = "Manually trigger Keycloak ID sync for debugging.")
    public ResponseEntity<ApiResponse<Void>> syncKeycloakId(@PathVariable Long id, @PathVariable String keycloakId) {
      log.info("DEBUG: Endpoint /sync-keycloak-id called for id: {} with keycloakId: {}", id, keycloakId);
      userService.syncKeycloakId(id, keycloakId);
      return ResponseEntity.ok(ApiResponse.success("Keycloak ID sync attempted. Check logs.", null));
    }

    @GetMapping("/identity-types")
    @Operation(summary = "Get available identity types", description = "Retrieve list of valid seller identity types with their display labels and descriptions.")
    public ResponseEntity<ApiResponse<java.util.List<com.eshop.app.dto.response.SellerIdentityTypeResponse>>> getIdentityTypes() {
        java.util.List<com.eshop.app.dto.response.SellerIdentityTypeResponse> types = java.util.Arrays.stream(com.eshop.app.enums.SellerIdentityType.values())
                .map(type -> com.eshop.app.dto.response.SellerIdentityTypeResponse.builder()
                        .type(type.name())
                        .label(type.getDisplayName())
                        .description(type.getDescription())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(types));
    }

    @GetMapping("/business-types")
    @Operation(summary = "Get available business types", description = "Retrieve list of valid seller business types (categories) with their display labels.")
    public ResponseEntity<ApiResponse<java.util.List<com.eshop.app.dto.response.SellerBusinessTypeResponse>>> getBusinessTypes() {
        java.util.List<com.eshop.app.dto.response.SellerBusinessTypeResponse> types = java.util.Arrays.stream(com.eshop.app.enums.SellerBusinessType.values())
                .map(type -> com.eshop.app.dto.response.SellerBusinessTypeResponse.builder()
                        .type(type.name())
                        .label(type.getDisplayName())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(types));
    }
}
