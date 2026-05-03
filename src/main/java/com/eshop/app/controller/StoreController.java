package com.eshop.app.controller;

import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.service.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.eshop.app.constants.ApiConstants;

@Tag(name = "Stores", description = "Multi-vendor store management - sellers can create and manage their stores")
@RestController
@RequestMapping(ApiConstants.Endpoints.STORES)
public class StoreController {
    
    private final StoreService storeService;
    
    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Create new store",
        description = "Create a new store. Available for SELLER and ADMIN roles.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request) {
        StoreResponse response = storeService.createStore(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created successfully", response));
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Update store",
        description = "Update store information. Sellers can update their own store, admins can update any store.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Parameter(description = "Store ID") @PathVariable Long id,
            @Valid @RequestBody StoreCreateRequest request) {
        StoreResponse response = storeService.updateStore(id, request);
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", response));
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete store (Admin only)",
        description = "Delete a store. All products in this store will also be removed.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> deleteStore(
            @Parameter(description = "Store ID") @PathVariable Long id) {
        storeService.deleteStore(id);
        return ResponseEntity.ok(ApiResponse.success("Store deleted successfully", null));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable Long id) {
        StoreResponse response = storeService.getStoreById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/my-store")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore() {
        StoreResponse response = storeService.getMyStore();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<StoreResponse>>> getAllStores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "storeName") String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        PageResponse<StoreResponse> response = storeService.getAllStores(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<StoreResponse>>> searchStores(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<StoreResponse> response = storeService.searchStores(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
