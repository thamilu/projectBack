package com.eshop.app.controller;

import com.eshop.app.common.controller.BaseController;
import com.eshop.app.common.util.ETagGenerator;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.dto.request.*;
import com.eshop.app.dto.response.*;
import com.eshop.app.service.ProductService;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.observation.annotation.Observed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;


/**
 * Product management REST API controller.
 * 
 * <p>Enterprise-grade CRUD operations for products with:
 * <ul>
 *   <li>Role-based access control (SELLER, ADMIN)</li>
 *   <li>Batch operations with comprehensive error tracking</li>
 *   <li>Advanced search and filtering with caching</li>
 *   <li>Rate limiting and circuit breaker protection</li>
 *   <li>ETag support for conditional requests</li>
 *   <li>Comprehensive audit logging</li>
 *   <li>Consistent response types with proper HTTP status codes</li>
 * </ul>
 * 
 * @author E-Shop Team
 * @version 2.0.0
 * @since 1.0.0
 */
@Tag(name = "Products", description = "Product management endpoints - CRUD operations, search, filtering")
// - Rate limiting via @RateLimiter (Resilience4j)
// - ETag and If-Match/If-None-Match for safe updates
// - Audit logging and metrics for all major actions
//
// See API_DOCUMENTATION.md and ARCHITECTURE.md for details.
@RestController
@RequestMapping(ApiConstants.Endpoints.PRODUCTS)
@Validated
@Slf4j
@lombok.RequiredArgsConstructor
@CacheConfig(cacheNames = "products")
@Observed(name = "product.controller")
public class ProductController extends BaseController {

    /**
     * Security & Configuration Review Checklist:
     * - Rate limiting: Configured via @RateLimiter annotations on sensitive endpoints
     * - Input validation: Applied via @Valid, @Positive, etc. on all DTOs and parameters
     * - Authorization: RBAC checks via @PreAuthorize expressions for role and ownership validation
     * - Audit logging: User context and operation type logged for all major actions
     * - Security headers: CSP, X-Frame-Options configured in SecurityConfig
     * - CORS settings: Configured in WebMvcConfig for allowed origins
     */
    
    private final ProductService productService;
    private final ETagGenerator etagGenerator;
    
    // ==================== CREATE OPERATIONS ====================
    
    @PostMapping
    @PreAuthorize("hasAnyRole(@appProperties.security.roles.seller, @appProperties.security.roles.admin)")
    @Operation(
        summary = "Create new product",
        description = """
            Create a new product with the specified details.
            
            **Permissions:** SELLER, ADMIN
            
            **Notes:**
            - Tags are automatically created if they don't exist
            - SKU must be unique across all products
            - Friendly URL is auto-generated from the product name
            """,
        security = @SecurityRequirement(name = "Bearer Authentication"),
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "Product creation payload",
            content = @Content(schema = @Schema(implementation = com.eshop.app.dto.request.ProductCreateRequest.class))
        ),
        responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Product created successfully",
                content = @Content(schema = @Schema(implementation = com.eshop.app.dto.response.ProductResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Invalid input data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "Product with SKU already exists"
            )
        }
    )
    @RateLimiter(name = "productCreate")
    @Bulkhead(name = "productCreate", type = Bulkhead.Type.SEMAPHORE)
    @Caching(evict = {
        @CacheEvict(cacheNames = "productList", allEntries = true),
        @CacheEvict(cacheNames = "productCount", allEntries = true)
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = extractUserId(jwt);
        log.info("Creating product '{}' by user {}", request.getName(), userId);
        
        ProductResponse response = productService.createProduct(request, userId);
        
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(response.id())
            .toUri();
        
        return ResponseEntity
                .created(location)
                .body(ApiResponse.success("Product created successfully", response));
    }
    
    @PostMapping("/with-category")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Create product with auto category",
        description = "Creates a product. If categoryId is not provided, will create a new category with newCategoryName.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimiter(name = "productCreate")
    @Caching(evict = {
        @CacheEvict(cacheNames = "productList", allEntries = true),
        @CacheEvict(cacheNames = "productCount", allEntries = true)
    })
    public ApiResponse<ProductResponse> createProductWithCategory(
            @Valid @RequestBody ProductCreateWithCategoryRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = jwt != null ? jwt.getSubject() : "system";
        log.info("Creating product with auto category: {} by user {}", request.getName(), userId);
        
        ProductResponse response = productService.createProductWithAutoCategory(request);
        return ApiResponse.success("Product created successfully", response);
    }
    
    @PostMapping("/batch")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(
        summary = "Batch create products",
        description = "Create multiple products in a single request (max 100 products). Business logic handled in service layer with comprehensive error tracking.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @RateLimiter(name = "productBatchCreate")
    @CacheEvict(cacheNames = {"productList", "productCount"}, allEntries = true)
    public ResponseEntity<ApiResponse<BatchOperationResult<ProductResponse>>> createProductsBatch(
            @Valid @RequestBody BatchProductCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = extractUserId(jwt);
        log.info("Batch creating {} products by admin {}", request.products().size(), userId);
        
        // All logic delegated to service layer (SOLID principle)
        BatchOperationResult<ProductResponse> result = productService.createProductsBatch(request);
        
        String message = result.hasFailures() 
            ? "Batch creation completed with errors" 
            : "All products created successfully";
        
        HttpStatus status = result.isComplete() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        
        return ResponseEntity
                .status(status)
                .body(ApiResponse.success(message, result));
    }
    
    // ==================== UPDATE OPERATIONS ====================
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @productSecurityService.isOwner(#id, principal))")
    @Operation(
        summary = "Update product",
        description = "Update existing product. Supports conditional updates with If-Match header for optimistic locking.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @Caching(evict = {
        @CacheEvict(key = "#id"),
        @CacheEvict(cacheNames = "productList", allEntries = true)
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable @Positive Long id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : "system";
        log.info("Updating product {} by user {}", id, userId);
        // Validate ETag if provided
        if (ifMatch != null) {
            ProductResponse current = productService.getProductById(id);
            String currentEtag = etagGenerator.forTimestampedEntity(
                current.id(),
                current.updatedAt() != null ? current.updatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null
            );
            if (!currentEtag.equals(ifMatch)) {
                log.warn("ETag mismatch for product {}: expected {}, got {}", id, currentEtag, ifMatch);
                return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
            }
        }
        ProductResponse updated = productService.updateProduct(id, request);
        String newEtag = etagGenerator.forTimestampedEntity(
            updated.id(),
            updated.updatedAt() != null ? updated.updatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : null
        );
        return ResponseEntity.ok()
            .eTag(newEtag)
            .body(ApiResponse.success("Product updated successfully", updated));
    }
    
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(
        summary = "Update product stock",
        description = "Atomically update product stock quantity with operation type (SET, INCREMENT, DECREMENT). Single database operation returns updated product.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @CacheEvict(key = "#id")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable @Positive Long id,
            @Valid @RequestBody StockUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = extractUserId(jwt);
        log.info("Updating stock for product {}: {} {} by user {}", id, request.operation(), request.quantity(), userId);
        ProductResponse updated = productService.updateStockAndReturn(id, request);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", updated));
    }
    
    // ==================== DELETE OPERATIONS ====================
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or (hasRole('SELLER') and @productSecurityService.isOwner(#id, principal))")
    @Operation(
        summary = "Delete product",
        description = "Soft delete product by ID. Sellers can only delete their own products.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @CacheEvict(key = "#id")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt != null ? jwt.getSubject() : "system";
        log.info("Deleting product {} by user {}", id, userId);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole(@appProperties.security.roles.admin)")
    @Operation(
        summary = "Batch delete products",
        description = "Delete multiple products (max 100). Business logic in service layer with detailed error tracking.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @CacheEvict(cacheNames = {"products", "productList"}, allEntries = true)
    public ResponseEntity<ApiResponse<BatchOperationResult<Long>>> deleteProductsBatch(
            @Valid @RequestBody BatchDeleteRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        
        String userId = extractUserId(jwt);
        log.info("Batch deleting {} products by admin {}", request.ids().size(), userId);
        
        // All logic delegated to service layer (convert Set to List)
        BatchOperationResult<Long> result = productService.deleteProductsBatch(
            new java.util.ArrayList<>(request.ids()), 
            userId
        );
        
        String message = result.hasFailures() 
            ? "Batch delete completed with errors" 
            : "All products deleted successfully";
        
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }
    
    // ==================== READ OPERATIONS ====================
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get product by ID",
        description = "Retrieve product details by ID. Supports conditional requests via If-None-Match header for efficient caching."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "304", description = "Not modified (cached)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable @Positive Long id,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        
        ProductResponse response = productService.getProductById(id);
        
        // Use ETagGenerator utility for consistent, SHA-256 based ETags
        String etag = etagGenerator.forTimestampedEntity(
            response.id(), 
            response.updatedAt() != null ? response.updatedAt().atZone(java.time.ZoneOffset.UTC).toInstant() : java.time.Instant.now()
        );
        
        // Support conditional GET for efficient caching
        if (etagGenerator.matches(ifNoneMatch, etag, true)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }
        
        return ResponseEntity.ok()
                .eTag(etag)
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).mustRevalidate())
                .body(ApiResponse.success(response));
    }
    
    @GetMapping("/sku/{sku}")
    @Operation(
        summary = "Get product by SKU",
        description = "Retrieve product details by SKU code"
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product found"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(
            @PathVariable @NotBlank @Size(max = 50) String sku) {
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/url/{friendlyUrl}")
    @Operation(
        summary = "Get product by SEO URL",
        description = "Retrieve product by SEO-friendly URL slug"
    )
    public ResponseEntity<ApiResponse<ProductResponse>> getProductByFriendlyUrl(
            @PathVariable @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") String friendlyUrl) {
        ProductResponse response = productService.getProductByFriendlyUrl(friendlyUrl);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping
    @Operation(
        summary = "Get all products",
        description = "Retrieve paginated list of all active products. Automatic pagination validation applied."
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getAllProducts(
            @Valid @ParameterObject Pageable pageable) {
        PageResponse<ProductListResponse> response = productService.getAllProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    @RateLimiter(name = "search")
    @Operation(
        summary = "Advanced product search",
        description = "Search products with keyword filtering. Rate limited to prevent abuse.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> searchProducts(
            @RequestParam(required = false) @Size(min = 2, max = 100) String keyword,
            @ParameterObject Pageable pageable) {
        PageResponse<ProductListResponse> response = productService.searchProducts(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/category/{categoryId}")
    @Operation(
        summary = "Get products by category",
        description = "Retrieve all products in a specific category with pagination"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProductsByCategory(
            @PathVariable @Positive Long categoryId,
            @ParameterObject Pageable pageable) {
        PageResponse<ProductListResponse> response = productService.getProductsByCategory(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/brand/{brandId}")
    @Operation(
        summary = "Get products by brand",
        description = "Retrieve all products from a specific brand"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProductsByBrand(
            @PathVariable @Positive Long brandId,
            @ParameterObject Pageable pageable) {
        PageResponse<ProductListResponse> response = productService.getProductsByBrand(brandId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/store/{storeId}")
    @Operation(
        summary = "Get products by store",
        description = "Retrieve all products from a specific seller's store"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductListResponse>>> getProductsByStore(
            @PathVariable @Positive Long storeId,
            @ParameterObject Pageable pageable) {
        PageResponse<ProductListResponse> response = productService.getProductsByStore(storeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/featured")
    @Operation(
        summary = "Get featured products",
        description = "Retrieve featured/promoted products with pagination"
    )
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getFeaturedProducts(
            @ParameterObject Pageable pageable) {
        PageResponse<ProductResponse> response = productService.getFeaturedProducts(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/fulltext-search")
    @RateLimiter(name = "search")
    @Operation(
        summary = "Full-text product search",
        description = "Full-text search using PostgreSQL tsvector for name, description, and SKU. Rate limited to prevent abuse.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Search results"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "Too many requests")
    })
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> fullTextSearch(
            @RequestParam @Size(min = 2, max = 100) String query,
            @ParameterObject Pageable pageable) {
        PageResponse<ProductResponse> response = productService.fullTextSearch(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        try {
            long count = productService.getTotalProductCount();
            return ResponseEntity.ok("Product count: " + count);
        } catch (Exception e) {
            log.error("Test endpoint error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
