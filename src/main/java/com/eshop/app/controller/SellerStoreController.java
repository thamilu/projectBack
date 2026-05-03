package com.eshop.app.controller;

import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.StoreCreateRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.StoreResponse;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.StoreService;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/**
 * Seller Store Controller - Seller-specific storefront management.
 * 
 * <p>This controller provides endpoints at /seller/store for sellers to manage
 * their own store. It's aliased to the existing /shops endpoints but provides
 * a more intuitive path for the frontend.</p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Auto-resolves seller's store from JWT authentication</li>
 *   <li>Reuses existing ShopService business logic</li>
 *   <li>Maintains backward compatibility with /shops endpoints</li>
 *   <li>Follows RESTful conventions with semantic paths</li>
 *   <li>One-to-one relationship: Each seller can have only one store</li>
 * </ul>
 * 
 * <h2>Endpoints:</h2>
 * <ul>
 *   <li><b>GET /seller/store</b> - Get my store information</li>
 *   <li><b>POST /seller/store</b> - Create new store</li>
 *   <li><b>PUT /seller/store</b> - Update my store</li>
 *   <li><b>GET /seller/store/exists</b> - Check if I have a store</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 1.0
 * @since 2026-01-10
 */
@Tag(
        name = "Seller Stores", description = "Seller storefront management - Create, update, and manage seller stores"
)
@RestController
@RequestMapping(ApiConstants.BASE_PATH + "/seller/store")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Slf4j
public class SellerStoreController {
    
    private final StoreService storeService;
    
    /**
     * Get authenticated seller's store.
     * 
     * <p>Retrieves the store information for the currently authenticated seller.
     * The seller is automatically identified from the JWT token.</p>
     * 
     * @param jwt JWT token with seller authentication
     * @return seller's store information wrapped in ApiResponse
     * @throws ResourceNotFoundException if seller doesn't have a store yet
     */
    @GetMapping
    @Operation(
        summary = "Get my store",
        description = """
            Retrieve the authenticated seller's store information.
            
            **Permissions:** SELLER role required
            
            **Authentication:** JWT Bearer token required
            
            **Response:** Returns complete store details including:
            - Store ID, name, and description
            - Seller information
            - Store status and timestamps
            
            **Error Cases:**
            - 404: Seller doesn't have a store yet (need to create one first)
            - 401: Invalid or missing authentication token
            - 403: User doesn't have SELLER role
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Store retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Operation successful",
                      "data": {
                        "id": 1,
                        "storeName": "Tech Gadgets Store",
                        "description": "Premium electronics and gadgets",
                        "sellerId": 123,
                        "sellerName": "John Doe",
                        "createdAt": "2026-01-10T10:30:00",
                        "updatedAt": "2026-01-10T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Store not found - seller hasn't created a store yet",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user doesn't have SELLER role"
        )
    })
    public ResponseEntity<ApiResponse<StoreResponse>> getMyStore(
            Authentication authentication) {

        log.info("Fetching store for authenticated seller");

        StoreResponse store = storeService.getMyStore();

        return ResponseEntity.ok(ApiResponse.success(store));
    }
    
    /**
     * Create store for authenticated seller.
     * 
     * <p>Automatically associates the store with the authenticated seller.
     * Sellers can only have one store (one-to-one relationship).</p>
     * 
     * @param request store creation details (name, description, etc.)
     * @param jwt JWT token with seller authentication
     * @return created store information with HTTP 201 status
     * @throws ValidationException if request validation fails
     * @throws DuplicateResourceException if seller already has a store
     */
    @PostMapping
    @Operation(
        summary = "Create my store",
        description = """
            Create a new store for the authenticated seller.
            
            **Permissions:** SELLER role required
            
            **Authentication:** JWT Bearer token required
            
            **Request Body Requirements:**
                    - storeName: Required, 3-100 characters
            - shopDescription: Optional, max 500 characters
            
            **Business Rules:**
            - Each seller can only have ONE store
            - Store is automatically associated with authenticated seller
            - Store name doesn't need to be globally unique
            
            **Success Response:**
            - HTTP 201 Created
            - Returns complete store details
            
            **Error Cases:**
            - 400: Validation failed (invalid input)
            - 401: Invalid or missing authentication token
            - 403: User doesn't have SELLER role
            - 409: Seller already has a store
            """,
        security = @SecurityRequirement(name = "Bearer Authentication"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Store creation request with store name and description",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StoreCreateRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "storeName": "Tech Gadgets Store",
                      "description": "Premium electronics and gadgets for tech enthusiasts"
                    }
                    """)
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Store created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Store created successfully",
                      "data": {
                        "id": 1,
                        "storeName": "Tech Gadgets Store",
                        "description": "Premium electronics and gadgets for tech enthusiasts",
                        "sellerId": 123,
                        "sellerName": "John Doe",
                        "createdAt": "2026-01-10T10:30:00",
                        "updatedAt": "2026-01-10T10:30:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - validation failed",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user doesn't have SELLER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Conflict - seller already has a store"
        )
    })
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request,
            Authentication authentication) {

        if (authentication.getPrincipal() instanceof com.eshop.app.security.PrincipalDetails pd) {
            request.setSellerId(pd.getId());
        } else {
            // Fallback for non-record principal if any (shouldn't happen with our config)
            throw new AccessDeniedException("User details not found in principal");
        }

        log.info("Creating store for seller ID: {}", request.getSellerId());

        log.debug("Auto-populated request: sellerId={}", request.getSellerId());
        
        StoreResponse store = storeService.createStore(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Store created successfully", store));
    }
    
    /**
     * Update authenticated seller's store.
     * 
     * <p>Updates the existing store information for the authenticated seller.
     * Automatically identifies which store to update based on seller's JWT.</p>
     * 
     * @param request updated store information (name, description, etc.)
     * @param jwt JWT token with seller authentication
     * @return updated store information
     * @throws ResourceNotFoundException if seller doesn't have a store
     * @throws ValidationException if request validation fails
     */
    @PutMapping
    @Operation(
        summary = "Update my store",
        description = """
            Update the authenticated seller's store information.
            
            **Permissions:** SELLER role required
            
            **Authentication:** JWT Bearer token required
            
            **Request Body:**
                    - storeName: Required, 3-100 characters
            - shopDescription: Optional, max 500 characters
            
            **Business Rules:**
            - Seller must have an existing store (use POST to create first)
            - Only the seller can update their own store
            - Partial updates not supported - all fields required
            
            **Success Response:**
            - HTTP 200 OK
            - Returns updated store details
            
            **Error Cases:**
            - 400: Validation failed (invalid input)
            - 401: Invalid or missing authentication token
            - 403: User doesn't have SELLER role
            - 404: Seller doesn't have a store yet
            """,
        security = @SecurityRequirement(name = "Bearer Authentication"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Updated store information",
            required = true,
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = StoreCreateRequest.class),
                examples = @ExampleObject(value = """
                    {
                      "storeName": "Tech Gadgets Store - Premium",
                      "description": "Updated description with new product lines and services"
                    }
                    """)
            )
        )
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Store updated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = @ExampleObject(value = """
                    {
                      "success": true,
                      "message": "Store updated successfully",
                      "data": {
                        "id": 1,
                        "storeName": "Tech Gadgets Store - Premium",
                        "description": "Updated description with new product lines and services",
                        "sellerId": 123,
                        "sellerName": "John Doe",
                        "createdAt": "2026-01-10T10:30:00",
                        "updatedAt": "2026-01-10T15:45:00"
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Bad Request - validation failed"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user doesn't have SELLER role"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Store not found - seller hasn't created a store yet"
        )
    })
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Valid @RequestBody StoreCreateRequest request,
            Authentication authentication) {

        log.info("Updating store for authenticated seller");
        
        // Get seller's current store to get the ID
        StoreResponse currentStore = storeService.getMyStore();
        
        // Update using the existing StoreService
        StoreResponse updatedStore = storeService.updateStore(currentStore.getId(), request);
        
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully", updatedStore));
    }
    
    /**
     * Health check endpoint for store availability.
     * 
     * <p>Lightweight endpoint to check if the authenticated seller has a store configured.
     * Useful for UI conditional rendering and onboarding flows.</p>
     * 
     * @param jwt JWT token with seller authentication
     * @return boolean indicating if seller has a store (true = exists, false = doesn't exist)
     */
    @GetMapping("/exists")
    @Operation(
        summary = "Check if store exists",
        description = """
            Check if the authenticated seller has a store configured.
            
            **Permissions:** SELLER role required
            
            **Authentication:** JWT Bearer token required
            
            **Use Cases:**
            - Frontend conditional rendering (show "Create Store" vs "Manage Store")
            - Onboarding workflow validation
            - Pre-flight checks before store-dependent operations
            
            **Response:**
            - Returns `true` if seller has a store
            - Returns `false` if seller doesn't have a store yet
            - Always returns HTTP 200 (never throws 404)
            
            **Note:** This is a lightweight check that doesn't return full store details.
            Use GET /seller/store to retrieve complete store information.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Check completed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ApiResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Store exists",
                        value = """
                            {
                              "success": true,
                              "message": "Operation successful",
                              "data": true
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Store doesn't exist",
                        value = """
                            {
                              "success": true,
                              "message": "Operation successful",
                              "data": false
                            }
                            """
                    )
                }
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "Forbidden - user doesn't have SELLER role"
        )
    })
    public ResponseEntity<ApiResponse<Boolean>> checkStoreExists(
            Authentication authentication) {

        // Check if authenticated seller has a store
        // No need to extract JWT - authentication is already verified by Spring Security
        try {
            storeService.getMyStore();
            return ResponseEntity.ok(ApiResponse.success(true));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }
    }
}
