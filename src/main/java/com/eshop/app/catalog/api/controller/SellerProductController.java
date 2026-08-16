package com.eshop.app.catalog.api.controller;

import com.eshop.app.catalog.api.response.ProductListResponse;
import com.eshop.app.catalog.api.response.ProductResponse;
import com.eshop.app.catalog.application.port.in.ProductUseCase;
import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.store.api.response.StoreResponse;
import com.eshop.app.store.application.port.in.StoreUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Seller Product Controller - Seller-specific product catalog/inventory dashboard endpoint. Scopes
 * reading operations automatically to the logged-in seller's store context to guarantee tenant
 * boundaries.
 *
 * @since 2.0.0
 */
@io.swagger.v3.oas.annotations.tags.Tag(
        name = "Seller Products",
        description = "Seller-specific product inventory management")
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole(@appProperties.security.roles.seller)")
@Slf4j
public class SellerProductController {

    private final ProductUseCase productService;
    private final StoreUseCase storeService;

    /**
     * Get products for the currently authenticated seller's store. Enforces multitenant data
     * isolation by resolving store boundaries on the server.
     *
     * @param pageable pagination and sorting parameters
     * @return paginated response of products scoped to the seller's store
     */
    @GetMapping
    @Operation(
            summary = "Get current seller's products",
            description =
                    "Retrieve paginated list of products for the authenticated seller's store"
                            + " context.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getMyProducts(
            @ParameterObject Pageable pageable) {

        log.info("Securely fetching products for authenticated seller storefront");

        // Dynamically resolve current seller's store context
        StoreResponse store = storeService.getMyStore();
        Long storeId = store.getId();

        log.info(
                "Successfully resolved active store context for seller. Store ID: {}, Store Name:"
                        + " '{}'",
                storeId,
                store.getStoreName());

        PageResponse<ProductListResponse> response =
                productService.getProductsByStore(storeId, pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Store products retrieved successfully", response));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole(@appProperties.security.roles.seller) and @productSecurityService.isOwner(#id, principal)")
    @Operation(summary = "Toggle seller product status", description = "Toggle product lifecycle status between ACTIVE and INACTIVE.", security = @SecurityRequirement(name = "Bearer Authentication"))
    public ResponseEntity<ApiResponse<ProductResponse>> toggleProductStatus(
            @PathVariable @jakarta.validation.constraints.Positive Long id) {
        log.info("Request to toggle status for product ID: {}", id);
        ProductResponse response = productService.toggleProductStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Product status toggled successfully", response));
    }
}
