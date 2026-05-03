package com.eshop.app.service.impl;

import com.eshop.app.dto.request.BrandRequest;
import com.eshop.app.dto.request.BrandSearchCriteria;
import com.eshop.app.dto.response.BrandDetailResponse;
import com.eshop.app.dto.response.BrandResponse;
import com.eshop.app.dto.response.BrandSummaryResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.entity.Brand;
import com.eshop.app.exception.ConflictException;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.BrandMapper;
import com.eshop.app.repository.BrandRepository;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.service.BrandService;
import com.eshop.app.util.SlugUtils;
import com.eshop.app.util.SearchUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheNames = "brands")
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BrandMapper brandMapper;

    // ==================== CREATE OPERATIONS ====================

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse createBrand(BrandRequest request) {
        log.info("Creating brand: {}", request.getName());

        if (brandRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResourceAlreadyExistsException("Brand with name " + request.getName() + " already exists");
        }

        String slug = StringUtils.hasText(request.getSlug())
                ? request.getSlug()
                : SlugUtils.generateSlug(request.getName());

        if (brandRepository.existsBySlug(slug)) {
            slug = SlugUtils.generateUniqueSlug(slug, brandRepository::existsBySlug);
        }

        Brand brand = brandMapper.toEntity(request);
        brand.setSlug(slug);

        Brand saved = brandRepository.save(brand);
        return brandMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public List<BrandResponse> createBrandsBatch(List<BrandRequest> requests) {
        log.info("Batch creating {} brands", requests.size());

        List<String> names = requests.stream().map(BrandRequest::getName).toList();
        Set<String> uniqueNames = new HashSet<>(names);
        if (uniqueNames.size() != names.size()) {
            throw new ConflictException("Duplicate brand names in request");
        }

        List<String> existing = brandRepository.findExistingNames(names);
        if (!existing.isEmpty()) {
            throw new ResourceAlreadyExistsException("Brands already exist: " + String.join(", ", existing));
        }

        List<Brand> toSave = requests.stream()
                .map(r -> {
                    Brand b = brandMapper.toEntity(r);
                    b.setSlug(SlugUtils.generateSlug(r.getName()));
                    return b;
                }).toList();

        List<Brand> saved = brandRepository.saveAll(toSave);
        return saved.stream().map(brandMapper::toResponse).toList();
    }

    // ==================== READ OPERATIONS ====================

    @Override
    @Cacheable(key = "'id:' + #id")
    public BrandResponse getBrandById(Long id) {
        log.debug("Getting brand by ID: {}", id);
        Brand brand = findBrandOrThrow(id);
        return brandMapper.toResponse(brand);
    }

    @Override
    public Optional<BrandResponse> findBrandById(Long id) {
        return brandRepository.findById(id).map(brandMapper::toResponse);
    }

    @Override
    @Cacheable(key = "'detail:' + #id")
    public BrandDetailResponse getBrandDetailById(Long id) {
        log.debug("Getting brand detail by ID: {}", id);
        Brand brand = findBrandOrThrow(id);
        BrandDetailResponse resp = brandMapper.toDetailResponse(brand);

        resp.setProductCount(productRepository.countByBrandId(id));
        resp.setActiveProductCount(productRepository.countByBrandIdAndStatus(id, com.eshop.app.entity.enums.ProductStatus.ACTIVE));

        productRepository.findPriceStatsByBrandId(id).ifPresent(stats -> {
            resp.setAverageProductPrice(stats.getAveragePrice());
            resp.setMinProductPrice(stats.getMinPrice());
            resp.setMaxProductPrice(stats.getMaxPrice());
        });

        resp.setCategories(productRepository.findCategorySummariesByBrandId(id));
        return resp;
    }

    @Override
    @Cacheable(key = "'slug:' + #slug")
    public BrandResponse getBrandBySlug(String slug) {
        log.debug("Getting brand by slug: {}", slug);
        Brand brand = brandRepository.findBySlug(slug).orElseThrow(() -> new ResourceNotFoundException("Brand", "slug", slug));
        return brandMapper.toResponse(brand);
    }

    @Override
    public Optional<BrandResponse> findBrandByName(String name) {
        return brandRepository.findByNameIgnoreCase(name).map(brandMapper::toResponse);
    }

    @Override
    @Cacheable(key = "'all:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort")
    public PageResponse<BrandResponse> getAllBrands(Pageable pageable) {
        Page<Brand> page = brandRepository.findAll(pageable);
        return createPageResponse(page);
    }

    @Override
    @Cacheable(key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public PageResponse<BrandResponse> getActiveBrands(Pageable pageable) {
        Page<Brand> page = brandRepository.findByActiveTrue(pageable);
        return createPageResponse(page);
    }

    @Override
    @Cacheable(key = "'dropdown'")
    public List<BrandSummaryResponse> getAllBrandsForDropdown() {
        return brandRepository.findAllActiveBrandsSummary();
    }

    @Override
    public PageResponse<BrandResponse> getBrandsByCategory(Long categoryId, Pageable pageable) {
        Page<Brand> page = brandRepository.findByCategoryId(categoryId, pageable);
        return createPageResponse(page);
    }

    @Override
    @Cacheable(key = "'featured:' + #limit")
    public List<BrandResponse> getFeaturedBrands(int limit) {
        return brandRepository.findFeaturedBrands(Pageable.ofSize(limit))
                .stream().map(brandMapper::toResponse).toList();
    }

    // ==================== SEARCH ====================

    @Override
    public PageResponse<BrandResponse> searchBrands(String keyword, Pageable pageable) {
        String safe = SearchUtils.sanitizeStrict(keyword);
        Page<Brand> page = brandRepository.searchByKeyword(safe, pageable);
        return createPageResponse(page);
    }

    @Override
    public PageResponse<BrandResponse> searchBrands(BrandSearchCriteria criteria, Pageable pageable) {
        Page<Brand> page = brandRepository.searchWithCriteria(criteria, pageable);
        return createPageResponse(page);
    }

    @Override
    public List<String> autocompleteBrandNames(String prefix, int limit) {
        String safe = SearchUtils.sanitizeStrict(prefix);
        return brandRepository.findBrandNamesByPrefix(safe, Pageable.ofSize(limit));
    }

    // ==================== UPDATE ====================

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        log.info("Updating brand: {}", id);
        Brand brand = findBrandOrThrow(id);

        if (!brand.getName().equalsIgnoreCase(request.getName()) &&
                brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new ResourceAlreadyExistsException("Brand with name " + request.getName() + " already exists");
        }

        if (!brand.getName().equalsIgnoreCase(request.getName())) {
            String newSlug = StringUtils.hasText(request.getSlug()) ? request.getSlug() : SlugUtils.generateSlug(request.getName());
            if (brandRepository.existsBySlugAndIdNot(newSlug, id)) {
                newSlug = SlugUtils.generateUniqueSlug(newSlug, s -> brandRepository.existsBySlugAndIdNot(s, id));
            }
            brand.setSlug(newSlug);
        }

        brandMapper.updateEntity(brand, request);
        Brand updated = brandRepository.save(brand);
        return brandMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse partialUpdateBrand(Long id, BrandRequest request) {
        log.info("Partially updating brand: {}", id);
        Brand brand = findBrandOrThrow(id);

        if (StringUtils.hasText(request.getName())) {
            if (!brand.getName().equalsIgnoreCase(request.getName()) &&
                    brandRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
                throw new ResourceAlreadyExistsException("Brand with name " + request.getName() + " already exists");
            }
            brand.setName(request.getName());
            brand.setSlug(SlugUtils.generateSlug(request.getName()));
        }

        if (StringUtils.hasText(request.getDescription())) brand.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getLogoUrl())) brand.setLogoUrl(request.getLogoUrl());
        if (StringUtils.hasText(request.getWebsiteUrl())) brand.setWebsiteUrl(request.getWebsiteUrl());
        if (request.getActive() != null) brand.setActive(request.getActive());
        if (request.getFeatured() != null) brand.setFeatured(request.getFeatured());

        Brand updated = brandRepository.save(brand);
        return brandMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse updateBrandLogo(Long id, String logoUrl) {
        Brand brand = findBrandOrThrow(id);
        brand.setLogoUrl(logoUrl);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse toggleBrandStatus(Long id, boolean active) {
        Brand brand = findBrandOrThrow(id);
        brand.setActive(active);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse toggleBrandFeatured(Long id, boolean featured) {
        Brand brand = findBrandOrThrow(id);
        brand.setFeatured(featured);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    // ==================== DELETE ====================

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public void deleteBrand(Long id) {
        Brand brand = findBrandOrThrow(id);
        long productCount = productRepository.countByBrandId(id);
        if (productCount > 0) {
            throw new ConflictException("Cannot delete brand '" + brand.getName() + "'. It has " + productCount + " associated products.");
        }
        brandRepository.delete(brand);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public void softDeleteBrand(Long id) {
        Brand brand = findBrandOrThrow(id);
        brand.setActive(false);
        brand.setDeleted(true);
        brand.setDeletedAt(LocalDateTime.now());
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public void deleteBrandsBatch(Set<Long> ids) {
        List<Brand> brands = brandRepository.findAllById(ids);
        if (brands.size() != ids.size()) {
            Set<Long> found = brands.stream().map(Brand::getId).collect(Collectors.toSet());
            Set<Long> notFound = new HashSet<>(ids);
            notFound.removeAll(found);
            throw new ResourceNotFoundException("Brands not found: " + notFound);
        }

        Map<Long, Long> productCounts = productRepository.countByBrandIds(ids);
        List<String> brandsWithProducts = brands.stream()
                .filter(b -> productCounts.getOrDefault(b.getId(), 0L) > 0)
                .map(b -> String.format("%s (%d products)", b.getName(), productCounts.get(b.getId())))
                .toList();

        if (!brandsWithProducts.isEmpty()) {
            throw new ConflictException("Cannot delete brands with products: " + String.join(", ", brandsWithProducts));
        }

        brandRepository.deleteAll(brands);
    }

    @Override
    @Transactional
    @CacheEvict(allEntries = true)
    public BrandResponse restoreBrand(Long id) {
        Brand brand = brandRepository.findByIdIncludeDeleted(id).orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
        brand.setActive(true);
        brand.setDeleted(false);
        brand.setDeletedAt(null);
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    // ==================== VALIDATION/STATISTICS ====================

    @Override
    public boolean existsByName(String name) {
        return brandRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long excludeId) {
        return brandRepository.existsByNameIgnoreCaseAndIdNot(name, excludeId);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return brandRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsById(Long id) {
        return brandRepository.existsById(id);
    }

    @Override
    @Cacheable(key = "'count:total'")
    public long getTotalBrandCount() {
        return brandRepository.count();
    }

    @Override
    @Cacheable(key = "'count:active'")
    public long getActiveBrandCount() {
        return brandRepository.countByActiveTrue();
    }

    @Override
    public long getProductCountByBrand(Long brandId) {
        return productRepository.countByBrandId(brandId);
    }

    // ==================== HELPERS ====================

    private Brand findBrandOrThrow(Long id) {
        return brandRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Brand", "id", id));
    }

    private PageResponse<BrandResponse> createPageResponse(Page<Brand> brandPage) {
        return PageResponse.of(brandPage, brandMapper::toResponse);
    }
}
