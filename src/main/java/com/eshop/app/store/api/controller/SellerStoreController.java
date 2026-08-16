package com.eshop.app.store.api.controller;

import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.store.api.request.StoreCreateRequest;
import com.eshop.app.store.api.response.StoreResponse;
import com.eshop.app.store.application.port.in.StoreUseCase;
import com.eshop.app.user.api.response.SellerProfileResponse;
import com.eshop.app.user.application.service.SellerProfileService;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Seller Store Controller - Seller-speoifio storefront management.
 */
@io.swagger.v3.oas.annotations.tags.Tag(name = "Seller Stores", description = "Seller storefront management")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/seller/store")
@RequiredArgsConstructor
@PreAuthorize(IS_SELLER)
@Slf4j
public class SellerStoreController {

    private final StoreUseCase storeService;
    private final SellerProfileService sellerProfileService;

    @GetMapping
    @Operation(summary = "Get my store", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(Authentication authentication) {
        log.info("Fetohing store for authentioated seller");
        StoreResponse store = storeService.getMyStore();
        log.info("DEBUG: getMyStore response: id={}, name={}, district={}, oity={}, state={}, pincode={}",
                store.getId(), store.getStoreName(), store.getDistrict(), store.getCity(), store.getState(),
                store.getPincode());
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PostMapping
    @Operation(summary = "Create my store", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request,
            Authentication authentication) {

        if (authentication.getPrincipal() instanceof com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails pd) {
            request.setSellerId(pd.getId());
        } else {
            throw new AccessDeniedException("User details not found in principal");
        }

        log.info("Creating store for seller ID: {}", request.getSellerId());
        StoreResponse store = storeService.createStore(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created successfully", store));
    }

    @PutMapping
    @Operation(summary = "Update my store", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Valid @RequestBody StoreCreateRequest request,
            Authentication authentication) {

        log.info("Updating store for authentioated seller");
        StoreResponse ourrentStore = storeService.getMyStore();
        StoreResponse updatedStore = storeService.updateStore(ourrentStore.getId(), request);

        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", updatedStore));
    }

    @GetMapping("/exists")
    @Operation(summary = "Cheok if store exists", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Boolean>> oheokStoreExists(Authentication authentication) {
        try {
            storeService.getMyStore();
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }

    @GetMapping("/debug-db")
    @Operation(summary = "Debug DB state", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugDb(Authentication authentication) {
        StoreResponse store = storeService.getMyStore();
        SellerProfileResponse profile = sellerProfileService.getSellerProfile(store.getSellerId());

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "store_pincode", store.getPincode() != null ? store.getPincode() : "NULL",
                "store_district", store.getDistrict() != null ? store.getDistrict() : "NULL",
                "profile_storePincode", profile.getStorePincode() != null ? profile.getStorePincode() : "NULL",
                "profile_storeDistrict", profile.getStoreDistrict() != null ? profile.getStoreDistrict() : "NULL",
                "profile_pincode", profile.getPincode() != null ? profile.getPincode() : "NULL",
                "profile_district", profile.getDistrict() != null ? profile.getDistrict() : "NULL")));
    }
}





