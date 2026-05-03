package com.eshop.app.controller;

import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.dto.response.SellerProfileResponse;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.service.StoreService;
import com.eshop.app.service.SellerProfileService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Seller Store Controller - Seller-specific storefront management.
 */
@Tag(name = "Seller Stores", description = "Seller storefront management")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/seller/store")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Slf4j
public class SellerStoreController {
    
    private final StoreService storeService;
    private final SellerProfileService sellerProfileService;
    
    @GetMapping
    @Operation(summary = "Get my store", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(Authentication authentication) {
        log.info("Fetching store for authenticated seller");
        StoreResponse store = storeService.getMyStore();
        log.info("DEBUG: getMyStore response: id={}, name={}, district={}, city={}, state={}, pincode={}", 
            store.getId(), store.getStoreName(), store.getDistrict(), store.getCity(), store.getState(), store.getPincode());
        return ResponseEntity.ok(ApiResponse.success(store));
    }
    
    @PostMapping
    @Operation(summary = "Create my store", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request,
            Authentication authentication) {

        if (authentication.getPrincipal() instanceof com.eshop.app.security.PrincipalDetails pd) {
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

        log.info("Updating store for authenticated seller");
        StoreResponse currentStore = storeService.getMyStore();
        StoreResponse updatedStore = storeService.updateStore(currentStore.getId(), request);
        
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", updatedStore));
    }
    
    @GetMapping("/exists")
    @Operation(summary = "Check if store exists", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<Boolean>> checkStoreExists(Authentication authentication) {
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
            "profile_district", profile.getDistrict() != null ? profile.getDistrict() : "NULL"
        )));
    }
}
