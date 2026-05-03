package com.eshop.app.service;

import com.eshop.app.dto.request.BrandRequest;
import com.eshop.app.dto.request.BrandSearchCriteria;
import com.eshop.app.dto.response.BrandResponse;
import com.eshop.app.dto.response.BrandDetailResponse;
import com.eshop.app.dto.response.BrandSummaryResponse;
import com.eshop.app.dto.response.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service interface for Brand management operations.
 * <p>
 * Provides CRUD operations and advanced querying capabilities for brands.
 * All methods are transactional by default in the implementation.
 * </p>
 *
 * @author EShop Team
 * @version 1.0
 * @since 2024-01-01
 */
@Validated
public interface BrandService {

    // ==================== CREATE OPERATIONS ====================

    /**
     * Creates a new brand.
     *
     * @param request the brand creation request containing brand details
     * @return the created brand response
     * @throws com.eshop.app.exception.DuplicateResourceException if brand name already exists
     * @throws com.eshop.app.exception.ValidationException if request validation fails
     */
    BrandResponse createBrand(@Valid @NotNull BrandRequest request);

    /**
     * Creates multiple brands in a batch operation.
     *
     * @param requests list of brand creation requests
     * @return list of created brand responses
     * @throws com.eshop.app.exception.BatchOperationException if any creation fails
     */
    List<BrandResponse> createBrandsBatch(@Valid @Size(max = 100) List<BrandRequest> requests);

    // ==================== READ OPERATIONS ====================

    /**
     * Retrieves a brand by its ID.
     *
     * @param id the brand ID
     * @return the brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    BrandResponse getBrandById(@NotNull @Positive Long id);

    /**
     * Retrieves a brand by its ID with optional loading.
     *
     * @param id the brand ID
     * @return Optional containing the brand if found
     */
    Optional<BrandResponse> findBrandById(@NotNull @Positive Long id);

    /**
     * Retrieves detailed brand information including product count and statistics.
     *
     * @param id the brand ID
     * @return detailed brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    BrandDetailResponse getBrandDetailById(@NotNull @Positive Long id);

    /**
     * Retrieves a brand by its unique slug.
     *
     * @param slug the brand's URL-friendly slug
     * @return the brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    BrandResponse getBrandBySlug(@NotBlank String slug);

    /**
     * Retrieves a brand by its exact name (case-insensitive).
     *
     * @param name the brand name
     * @return Optional containing the brand if found
     */
    Optional<BrandResponse> findBrandByName(@NotBlank String name);

    /**
     * Retrieves all brands with pagination.
     *
     * @param pageable pagination information
     * @return paginated brand responses
     */
    PageResponse<BrandResponse> getAllBrands(Pageable pageable);

    /**
     * Retrieves all active brands with pagination.
     *
     * @param pageable pagination information
     * @return paginated active brand responses
     */
    PageResponse<BrandResponse> getActiveBrands(Pageable pageable);

    /**
     * Retrieves all brands as a simple list (for dropdowns, no pagination).
     * Limited to active brands only.
     *
     * @return list of brand summary responses
     */
    List<BrandSummaryResponse> getAllBrandsForDropdown();

    /**
     * Retrieves brands by category ID.
     *
     * @param categoryId the category ID
     * @param pageable pagination information
     * @return paginated brand responses for the category
     */
    PageResponse<BrandResponse> getBrandsByCategory(
            @NotNull @Positive Long categoryId, 
            Pageable pageable);

    /**
     * Retrieves featured/popular brands.
     *
     * @param limit maximum number of brands to return
     * @return list of featured brand responses
     */
    List<BrandResponse> getFeaturedBrands(@Positive int limit);

    // ==================== SEARCH OPERATIONS ====================

    /**
     * Searches brands by keyword in name and description.
     *
     * @param keyword the search keyword
     * @param pageable pagination information
     * @return paginated search results
     */
    PageResponse<BrandResponse> searchBrands(
            @NotBlank @Size(min = 2, max = 100) String keyword, 
            Pageable pageable);

    /**
     * Advanced search with multiple criteria.
     *
     * @param criteria search criteria object
     * @param pageable pagination information
     * @return paginated search results
     */
    PageResponse<BrandResponse> searchBrands(
            @Valid BrandSearchCriteria criteria, 
            Pageable pageable);

    /**
     * Autocomplete search for brand names.
     *
     * @param prefix the prefix to search for
     * @param limit maximum number of results
     * @return list of matching brand names
     */
    List<String> autocompleteBrandNames(
            @NotBlank @Size(min = 1, max = 50) String prefix, 
            @Positive int limit);

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Updates an existing brand.
     *
     * @param id the brand ID
     * @param request the brand update request
     * @return the updated brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     * @throws com.eshop.app.exception.DuplicateResourceException if new name already exists
     */
    BrandResponse updateBrand(
            @NotNull @Positive Long id, 
            @Valid @NotNull BrandRequest request);

    /**
     * Partially updates a brand (only non-null fields).
     *
     * @param id the brand ID
     * @param request the partial update request
     * @return the updated brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    BrandResponse partialUpdateBrand(
            @NotNull @Positive Long id, 
            @Valid BrandRequest request);

    /**
     * Updates brand logo/image.
     *
     * @param id the brand ID
     * @param logoUrl the new logo URL
     * @return the updated brand response
     */
    BrandResponse updateBrandLogo(
            @NotNull @Positive Long id, 
            @NotBlank String logoUrl);

    /**
     * Toggles brand active status.
     *
     * @param id the brand ID
     * @param active the new active status
     * @return the updated brand response
     */
    BrandResponse toggleBrandStatus(
            @NotNull @Positive Long id, 
            boolean active);

    /**
     * Toggles brand featured status.
     *
     * @param id the brand ID
     * @param featured the new featured status
     * @return the updated brand response
     */
    BrandResponse toggleBrandFeatured(
            @NotNull @Positive Long id, 
            boolean featured);

    // ==================== DELETE OPERATIONS ====================

    /**
     * Deletes a brand by ID.
     * Will fail if brand has associated products.
     *
     * @param id the brand ID
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     * @throws com.eshop.app.exception.BusinessException if brand has products
     */
    void deleteBrand(@NotNull @Positive Long id);

    /**
     * Soft deletes a brand (marks as inactive).
     *
     * @param id the brand ID
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    void softDeleteBrand(@NotNull @Positive Long id);

    /**
     * Deletes multiple brands in batch.
     *
     * @param ids set of brand IDs to delete
     * @throws com.eshop.app.exception.BatchOperationException if any deletion fails
     */
    void deleteBrandsBatch(@Size(max = 100) Set<@Positive Long> ids);

    /**
     * Restores a soft-deleted brand.
     *
     * @param id the brand ID
     * @return the restored brand response
     * @throws com.eshop.app.exception.ResourceNotFoundException if brand not found
     */
    BrandResponse restoreBrand(@NotNull @Positive Long id);

    // ==================== VALIDATION OPERATIONS ====================

    /**
     * Checks if a brand name already exists.
     *
     * @param name the brand name to check
     * @return true if name exists, false otherwise
     */
    boolean existsByName(@NotBlank String name);

    /**
     * Checks if a brand name exists excluding a specific brand ID.
     *
     * @param name the brand name to check
     * @param excludeId the brand ID to exclude from check
     * @return true if name exists for another brand
     */
    boolean existsByNameAndIdNot(@NotBlank String name, @NotNull Long excludeId);

    /**
     * Checks if a brand slug already exists.
     *
     * @param slug the slug to check
     * @return true if slug exists, false otherwise
     */
    boolean existsBySlug(@NotBlank String slug);

    /**
     * Validates if a brand ID exists.
     *
     * @param id the brand ID
     * @return true if brand exists
     */
    boolean existsById(@NotNull @Positive Long id);

    // ==================== STATISTICS OPERATIONS ====================

    /**
     * Gets the total count of brands.
     *
     * @return total brand count
     */
    long getTotalBrandCount();

    /**
     * Gets the count of active brands.
     *
     * @return active brand count
     */
    long getActiveBrandCount();

    /**
     * Gets product count for a brand.
     *
     * @param brandId the brand ID
     * @return number of products for the brand
     */
    long getProductCountByBrand(@NotNull @Positive Long brandId);
}