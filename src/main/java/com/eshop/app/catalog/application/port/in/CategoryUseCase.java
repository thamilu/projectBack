package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.response.CategoryResponse;
import com.eshop.app.catalog.api.response.CategoryTreeResponse;
import com.eshop.app.catalog.api.request.CategoryRequest;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Inbound Port for Category Use Cases.
 */
public interface CategoryUseCase {
    List<CategoryResponse> getAllCategories();
    PageResponse<CategoryResponse> getAllCategories(Pageable pageable);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse createCategory(String name);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    PageResponse<CategoryResponse> searchCategories(String keyword, Pageable pageable);
    void deleteCategory(Long id);
    List<CategoryResponse> createCategories(List<CategoryRequest> requests);
    void hardDeleteCategory(Long id);
    void softDeleteCategory(Long id);
    CategoryResponse restoreCategory(Long id);
    List<CategoryTreeResponse> getCategoryTree();
    List<CategoryResponse> getSubcategories(Long id);
    List<CategoryResponse> getCategoryPath(Long id);
    List<CategoryResponse> getRootCategories();
}

