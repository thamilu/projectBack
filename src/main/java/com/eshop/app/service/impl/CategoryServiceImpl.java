package com.eshop.app.service.impl;

import com.eshop.app.dto.request.CategoryRequest;
import com.eshop.app.dto.response.CategoryResponse;
import com.eshop.app.dto.response.CategoryTreeResponse;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.entity.Category;
import com.eshop.app.exception.ResourceAlreadyExistsException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.CategoryMapper;
import com.eshop.app.repository.CategoryRepository;
import com.eshop.app.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import static com.eshop.app.config.CacheConfig.CATEGORY_LIST_CACHE;
import static com.eshop.app.config.CacheConfig.CATEGORIES_CACHE;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.Objects;

import java.util.List;
// ...existing code...

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    
    @Override
    @Transactional
    @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        log.info("Creating category with name: {}", request.getName());
        validateCategoryRequest(request);
        if (categoryRepository.existsByName(request.getName().trim())) {
            log.warn("Category creation failed - duplicate name: {}", request.getName());
            throw new ResourceAlreadyExistsException("Category with name '" + request.getName() + "' already exists");
        }
        Category category = buildCategoryFromRequest(request);
        if (request.getParentId() != null) {
            Category parent = findCategoryByIdOrThrow(request.getParentId());
            category.setParent(parent);
        }
        category = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", category.getId());
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse createCategory(String name) {
        CategoryRequest req = CategoryRequest.builder().name(name).build();
        return createCategory(req);
    }
    
    @Override
    @Transactional
    @Caching(
        put = @CachePut(value = CATEGORIES_CACHE, key = "#id"),
        evict = @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    )
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category with id: {}", id);
        validateCategoryRequest(request);
        Category category = findCategoryByIdOrThrow(id);
        String newName = request.getName().trim();
        if (!Objects.equals(category.getName(), newName) && categoryRepository.existsByName(newName)) {
            log.warn("Category update failed - duplicate name: {}", newName);
            throw new ResourceAlreadyExistsException("Category with name '" + newName + "' already exists");
        }
        updateCategoryFromRequest(category, request);
        category = categoryRepository.save(category);
        log.info("Category updated successfully: {}", id);
        return categoryMapper.toCategoryResponse(category);
    }
    
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CATEGORIES_CACHE, key = "#id"),
        @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    })
    public void deleteCategory(Long id) {
        log.info("Deleting category with id: {}", id);
        Category category = findCategoryByIdOrThrow(id);
        // Soft delete
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category soft-deleted successfully: {}", id);
    }
    
    @Override
    @Cacheable(value = "categories", key = "#id", unless = "#result == null")
    public CategoryResponse getCategoryById(Long id) {
        log.debug("Fetching category by id: {}", id);
        Category category = findCategoryByIdOrThrow(id);
        return categoryMapper.toCategoryResponse(category);
    }
    
    @Override
    @Cacheable(
        value = CATEGORY_LIST_CACHE,
        key = "'page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize + '_sort_' + #pageable.sort.toString()",
        unless = "#result.content.isEmpty()"
    )
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {
        log.debug("Fetching all categories - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Category> categoryPage = categoryRepository.findAll(pageable);
        List<CategoryResponse> categories = categoryPage.getContent().stream()
            .map(categoryMapper::toCategoryResponse)
            .toList();
        log.debug("Found {} categories", categories.size());
        return PageResponse.of(categoryPage, categoryMapper::toCategoryResponse);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categoryList = categoryRepository.findAll();
        return categoryList.stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }
    
    @Override
    public PageResponse<CategoryResponse> searchCategories(String keyword, Pageable pageable) {
        log.debug("Searching categories with keyword: '{}', page: {}", keyword, pageable.getPageNumber());
        String sanitizedKeyword = sanitizeSearchKeyword(keyword);
        if (!StringUtils.hasText(sanitizedKeyword)) {
            log.debug("Empty keyword, returning all categories");
            return getAllCategories(pageable);
        }
        Page<Category> categoryPage = categoryRepository.searchCategories(sanitizedKeyword, pageable);
        List<CategoryResponse> categories = categoryPage.getContent().stream()
            .map(categoryMapper::toCategoryResponse)
            .toList();
        log.debug("Search found {} categories", categories.size());
        return PageResponse.of(categoryPage, categoryMapper::toCategoryResponse);
    }

    // Helper methods for validation, null safety, and sanitization
    private Category findCategoryByIdOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new ResourceNotFoundException("Category not found with id: " + id);
                });
    }

    private void validateCategoryRequest(CategoryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Category request cannot be null");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Category name cannot be empty");
        }
        if (request.getName().length() > 100) {
            throw new IllegalArgumentException("Category name cannot exceed 100 characters");
        }
    }

    private Category buildCategoryFromRequest(CategoryRequest request) {
        return Category.builder()
                .name(request.getName().trim())
                .description(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null)
                .imageUrl(StringUtils.hasText(request.getImageUrl()) ? request.getImageUrl().trim() : null)
                .active(true)
                .build();
    }

    private void updateCategoryFromRequest(Category category, CategoryRequest request) {
        category.setName(request.getName().trim());
        if (StringUtils.hasText(request.getDescription())) {
            category.setDescription(request.getDescription().trim());
        }
        if (StringUtils.hasText(request.getImageUrl())) {
            category.setImageUrl(request.getImageUrl().trim());
        }
    }

    private String sanitizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return "";
        }
        return keyword.trim().replaceAll("[%_]", "").replaceAll("\\s+", " ");
    }

    @Override
    @Transactional
    @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    public List<CategoryResponse> createCategories(List<CategoryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }
        return requests.stream()
                .map(this::createCategory)
                .toList();
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CATEGORIES_CACHE, allEntries = true),
        @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    })
    public void hardDeleteCategory(Long id) {
        log.info("Hard deleting category id={}", id);
        if (!categoryRepository.existsById(id)) {
            log.warn("Attempt to hard-delete non-existent category id={}", id);
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void softDeleteCategory(Long id) {
        // reuse existing soft-delete implementation
        deleteCategory(id);
    }

    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = CATEGORIES_CACHE, key = "#id"),
        @CacheEvict(value = CATEGORY_LIST_CACHE, allEntries = true)
    })
    public CategoryResponse restoreCategory(Long id) {
        log.info("Restoring category id={}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setActive(true);
        category = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> getCategoryTree() {
        List<Category> allCategories = categoryRepository.findAll();
        // Filter roots (parent is null)
        return allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(this::toCategoryTreeResponse)
                .toList();
    }

    private CategoryTreeResponse toCategoryTreeResponse(Category category) {
        CategoryTreeResponse response = new CategoryTreeResponse(category.getId(), category.getName());
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            response.setChildren(category.getSubCategories().stream()
                    .map(this::toCategoryTreeResponse)
                    .toList());
        }
        return response;
    }

    @Override
    public List<CategoryResponse> getSubcategories(Long id) {
        // Model currently has no parent-child relationship; return empty list for compatibility
        return List.of();
    }

    @Override
    public List<CategoryResponse> getCategoryPath(Long id) {
        // No hierarchical parent tracking - return single-element path
        Category category = findCategoryByIdOrThrow(id);
        return List.of(categoryMapper.toCategoryResponse(category));
    }

    @Override
    public List<CategoryResponse> getRootCategories() {
        // Without parent relationship, treat all active categories as roots
        List<Category> all = categoryRepository.findAll();
        return all.stream()
                .filter(Category::getActive)
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }
}
