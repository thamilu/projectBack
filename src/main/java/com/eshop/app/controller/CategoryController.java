package com.eshop.app.controller;

import com.eshop.app.dto.request.CategoryRequest;
import com.eshop.app.dto.response.ApiResponse;
import com.eshop.app.dto.response.CategoryResponse;
import com.eshop.app.dto.response.CategoryTreeResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.exception.InvalidParameterException;
import com.eshop.app.service.CategoryService;
import com.eshop.app.constants.ApiConstants;
import com.eshop.app.util.ControllerResponseUtils;
import com.eshop.app.util.PaginationUtils;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Tag(name = "Categories", description = "Product category management with hierarchical support")
@RestController
@RequestMapping(value = ApiConstants.Endpoints.CATEGORIES, produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "id", "createdAt", "updatedAt");
    private static final int MAX_PAGE_SIZE = 1000;
    
    private final CategoryService categoryService;
    
    // ==================== ADMIN OPERATIONS ====================
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "category.create", description = "Time to create category")
    @Operation(
        summary = "Create new category (Admin only)",
        description = "Create a new product category with optional parent category for hierarchy.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request data")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category name already exists")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Admin '{}' creating category: {}", userDetails.getUsername(), request.getName());
        
        CategoryResponse response = categoryService.createCategory(request);
        
        log.info("Category created: id={}, name={}", response.getId(), response.getName());
        
        return ControllerResponseUtils.created("Category created successfully", response);
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "category.update", description = "Time to update category")
    @Operation(
        summary = "Update category (Admin only)",
        description = "Update an existing category. Cannot create circular parent-child relationships.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID", example = "1") 
            @PathVariable @Positive(message = "Category ID must be positive") Long id,
            @Valid @RequestBody CategoryRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Admin '{}' updating category: id={}", userDetails.getUsername(), id);
        
        CategoryResponse response = categoryService.updateCategory(id, request);
        
        log.info("Category updated: id={}, name={}", response.getId(), response.getName());
        
        return ControllerResponseUtils.ok("Category updated successfully", response);
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Timed(value = "category.delete", description = "Time to delete category")
    @Operation(
        summary = "Delete category (Admin only)",
        description = "Soft delete a category. Use hardDelete=true to permanently remove.",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @Parameter(description = "Category ID") 
            @PathVariable @Positive Long id,
            @Parameter(description = "Permanently delete category")
            @RequestParam(defaultValue = "false") boolean hardDelete,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Admin '{}' deleting category: id={}, hardDelete={}", 
            userDetails.getUsername(), id, hardDelete);
        
        if (hardDelete) {
            categoryService.hardDeleteCategory(id);
        } else {
            categoryService.softDeleteCategory(id);
        }
        
        log.info("Category deleted: id={}", id);
        
        return ControllerResponseUtils.ok("Category deleted successfully", null);
    }
    
    @PostMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Restore soft-deleted category (Admin only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<CategoryResponse>> restoreCategory(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Admin '{}' restoring category: id={}", userDetails.getUsername(), id);
        
        CategoryResponse response = categoryService.restoreCategory(id);
        return ControllerResponseUtils.ok("Category restored successfully", response);
    }
    
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create multiple categories (Admin only)",
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> createCategories(
            @Valid @RequestBody @Size(min = 1, max = 50, message = "Must provide 1-50 categories") 
            List<CategoryRequest> requests,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Admin '{}' creating {} categories", userDetails.getUsername(), requests.size());
        
        List<CategoryResponse> responses = categoryService.createCategories(requests);
        return ControllerResponseUtils.created("Categories created successfully", responses);
    }
    
    // ==================== PUBLIC READ OPERATIONS ====================
    
    @GetMapping("/{id}")
    @Timed(value = "category.get", description = "Time to get category by ID")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID", example = "1")
            @PathVariable @Positive(message = "Category ID must be positive") Long id,
            WebRequest request) {
        
        CategoryResponse response = categoryService.getCategoryById(id);
        
        // ETag support for conditional requests
        String etag = generateETag(response);
        if (request.checkNotModified(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        
        return ResponseEntity.ok()
            .eTag(etag)
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(ApiResponse.success(response));
    }
    
    @GetMapping
    @Timed(value = "category.list", description = "Time to list categories")
    @Operation(summary = "Get all categories with pagination")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            
            @Parameter(description = "Page size (1-100)")
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size,
            
            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,
            
            @Parameter(description = "Sort direction (ASC/DESC)")
            @RequestParam(defaultValue = "ASC") String sortDirection) {
        
        // Validate and normalize sort field (deny invalid/attacker-supplied fields)
        validateSortField(sortBy);

        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDirection)
            .orElse(Sort.Direction.ASC);
        Pageable pageable = PaginationUtils.createPageable(page, size, sortBy, direction);
        
        PageResponse<CategoryResponse> response = categoryService.getAllCategories(pageable);
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(2, TimeUnit.MINUTES).cachePublic())
            .header(HttpHeaders.VARY, "Accept-Encoding")
            .body(ApiResponse.success(response));
    }
    
    @GetMapping("/search")
    @Timed(value = "category.search", description = "Time to search categories")
    @Operation(summary = "Search categories by keyword")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> searchCategories(
            @Parameter(description = "Search keyword (2-100 characters)")
            @RequestParam @NotBlank @Size(min = 2, max = 100) String keyword,
            
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        
        String sanitizedKeyword = sanitizeSearchKeyword(keyword);
        log.debug("Searching categories with keyword: '{}'", sanitizedKeyword);

        // Let the service decide on how to apply the wildcard search; pass sanitized input only.
        Pageable pageable = PaginationUtils.createPageableWithFieldDesc(page, size, "name");
        PageResponse<CategoryResponse> response =
            categoryService.searchCategories(sanitizedKeyword, pageable);
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic())
            .header(HttpHeaders.VARY, "Accept-Encoding")
            .body(ApiResponse.success(response));
    }
    
    // ==================== HIERARCHY OPERATIONS ====================
    
    @GetMapping("/tree")
    @Operation(summary = "Get full category tree structure")
    public ResponseEntity<ApiResponse<List<CategoryTreeResponse>>> getCategoryTree() {
        List<CategoryTreeResponse> tree = categoryService.getCategoryTree();
        
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePublic())
            .body(ApiResponse.success(tree));
    }
    
    @GetMapping("/{id}/subcategories")
    @Operation(summary = "Get direct subcategories of a category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSubcategories(
            @PathVariable @Positive Long id) {
        List<CategoryResponse> subcategories = categoryService.getSubcategories(id);
        return ControllerResponseUtils.ok(subcategories);
    }
    
    @GetMapping("/{id}/path")
    @Operation(summary = "Get category path from root to this category")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryPath(
            @PathVariable @Positive Long id) {
        List<CategoryResponse> path = categoryService.getCategoryPath(id);
        return ControllerResponseUtils.ok(path);
    }
    
    @GetMapping("/roots")
    @Operation(summary = "Get all root categories (no parent)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRootCategories() {
        List<CategoryResponse> roots = categoryService.getRootCategories();
        return ResponseEntity.ok()
            .cacheControl(CacheControl.maxAge(5, TimeUnit.MINUTES).cachePublic())
            .body(ApiResponse.success(roots));
    }
    
    // ==================== HELPER METHODS ====================
    
    private void validateSortField(String sortBy) {
        boolean allowed = ALLOWED_SORT_FIELDS.stream()
            .anyMatch(s -> s.equalsIgnoreCase(sortBy));
        if (!allowed) {
            throw new InvalidParameterException(
                "Invalid sort field '" + sortBy + "'. Allowed fields: " + ALLOWED_SORT_FIELDS);
        }
    }
    
    private String sanitizeSearchKeyword(String keyword) {
        if (keyword == null) return "";
        // Trim, remove SQL wildcard characters and basic XSS-sensitive chars.
        String cleaned = keyword.trim()
            .replaceAll("[%_\\[\\]\\\\]", "")  // Remove SQL wildcards and brackets
            .replaceAll("\\s+", " ")               // Normalize whitespace
            .replaceAll("[<>\"']", "");            // Remove potential XSS characters
        // Collapse multiple spaces and limit length to defend against huge payloads
        if (cleaned.length() > 200) {
            cleaned = cleaned.substring(0, 200);
        }
        return cleaned;
    }
    
    private String generateETag(CategoryResponse response) {
        return "\"" + response.getId() + "-" + 
            (response.getUpdatedAt() != null ? response.getUpdatedAt().hashCode() : 0) + "\"";
    }
}