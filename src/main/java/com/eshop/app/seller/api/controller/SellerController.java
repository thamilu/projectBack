package com.eshop.app.seller.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.seller.application.port.in.SellerRoleSyncUseCase;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.user.application.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Unified Seller Controller - Manages seller profiles for all seller types.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Sellers", description = "Seller profile management - Register, view, and update seller profiles")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/sellers")
@RequiredArgsConstructor
@Slf4j
public class SellerController {

  private final SellerRoleSyncUseCase sellerAdminService;
  private final UserService userService;

  @PostMapping("/{id}/sync-role")
  @Operation(summary = "Debug: Sync Seller Role", description = "Manually trigger Keycloak role assignment for debugging.", security = @SecurityRequirement(name = "Bearer Authentication"))
  public ResponseEntity<ApiResponse<Void>> syncSellerRole(@PathVariable Long id) {
    log.info("DEBUG: Endpoint /sync-role called for id: {}", id);
    sellerAdminService.syncSellerRole(id);
    return ResponseEntity.ok(ApiResponse.success("Role sync attempted. Check logs.", null));
  }

  @PostMapping("/{id}/sync-keycloak-id/{keycloakId}")
  @Operation(summary = "Debug: Sync Keycloak ID", description = "Manually trigger Keycloak ID sync for debugging.")
  public ResponseEntity<ApiResponse<Void>> syncKeycloakId(@PathVariable Long id, @PathVariable String keycloakId) {
    log.info("DEBUG: Endpoint /sync-keycloak-id called for id: {} with keycloakId: {}", id, keycloakId);
    userService.syncKeycloakId(id, keycloakId);
    return ResponseEntity.ok(ApiResponse.success("Keycloak ID sync attempted. Check logs.", null));
  }

}
