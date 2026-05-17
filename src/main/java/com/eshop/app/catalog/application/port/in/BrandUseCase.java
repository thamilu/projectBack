package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.request.BrandRequest;
import com.eshop.app.catalog.api.response.BrandDetailResponse;
import com.eshop.app.catalog.api.response.BrandResponse;
import com.eshop.app.catalog.api.response.BrandSearchCriteria;
import com.eshop.app.catalog.api.response.BrandSummaryResponse;
import com.eshop.app.core.api.response.PageResponse;

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
 * Inbound Port for Brand Use Cases.
 */
@Validated
public interface BrandUseCase {

    BrandResponse createBrand(@Valid @NotNull BrandRequest request);

    List<BrandResponse> createBrandsBatch(@Valid @Size(max = 100) List<BrandRequest> requests);

    BrandResponse getBrandById(@NotNull @Positive Long id);

    Optional<BrandResponse> findBrandById(@NotNull @Positive Long id);

    BrandDetailResponse getBrandDetailById(@NotNull @Positive Long id);

    BrandResponse getBrandBySlug(@NotBlank String slug);

    Optional<BrandResponse> findBrandByName(@NotBlank String name);

    PageResponse<BrandResponse> getAllBrands(Pageable pageable);

    PageResponse<BrandResponse> getActiveBrands(Pageable pageable);

    List<BrandSummaryResponse> getAllBrandsForDropdown();

    PageResponse<BrandResponse> getBrandsByCategory(
                    @NotNull @Positive Long categoryId,
                    Pageable pageable);

    List<BrandResponse> getFeaturedBrands(@Positive int limit);

    PageResponse<BrandResponse> searchBrands(
                    @NotBlank @Size(min = 2, max = 100) String keyword,
                    Pageable pageable);

    PageResponse<BrandResponse> searchBrands(
                    @Valid BrandSearchCriteria criteria,
                    Pageable pageable);

    List<String> autocompleteBrandNames(
                    @NotBlank @Size(min = 1, max = 50) String prefix,
                    @Positive int limit);

    BrandResponse updateBrand(
                    @NotNull @Positive Long id,
                    @Valid @NotNull BrandRequest request);

    BrandResponse partialUpdateBrand(
                    @NotNull @Positive Long id,
                    @Valid BrandRequest request);

    BrandResponse updateBrandLogo(
                    @NotNull @Positive Long id,
                    @NotBlank String logoUrl);

    BrandResponse toggleBrandStatus(
                    @NotNull @Positive Long id,
                    boolean active);

    BrandResponse toggleBrandFeatured(
                    @NotNull @Positive Long id,
                    boolean featured);

    void deleteBrand(@NotNull @Positive Long id);

    void softDeleteBrand(@NotNull @Positive Long id);

    void deleteBrandsBatch(@Size(max = 100) Set<@Positive Long> ids);

    BrandResponse restoreBrand(@NotNull @Positive Long id);

    boolean existsByName(@NotBlank String name);

    boolean existsByNameAndIdNot(@NotBlank String name, @NotNull Long excludeId);

    boolean existsBySlug(@NotBlank String slug);

    boolean existsById(@NotNull @Positive Long id);

    long getTotalBrandCount();

    long getActiveBrandCount();

    long getProductCountByBrand(@NotNull @Positive Long brandId);
}

