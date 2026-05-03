package com.eshop.app.service;

import com.eshop.app.dto.request.CategoryRequest;
import com.eshop.app.dto.response.CategoryResponse;
import com.eshop.app.dto.response.CategoryTreeResponse;
import com.eshop.app.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAllCategories();
    PageResponse<CategoryResponse> getAllCategories(Pageable pageable);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse createCategory(String name);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    PageResponse<CategoryResponse> searchCategories(String keyword, Pageable pageable);
    void deleteCategory(Long id);
    
    List<CategoryResponse> createCategories(List<CategoryRequest> requests);

    /** Permanently remove category from datastore */
    void hardDeleteCategory(Long id);

    /** Soft-delete alias */
    void softDeleteCategory(Long id);

    /** Restore a previously soft-deleted category */
    CategoryResponse restoreCategory(Long id);

    /** Build a simple category tree (best-effort) */
    List<CategoryTreeResponse> getCategoryTree();

    List<CategoryResponse> getSubcategories(Long id);

    List<CategoryResponse> getCategoryPath(Long id);

    List<CategoryResponse> getRootCategories();
}
