package com.eshop.app.repository;

import com.eshop.app.entity.Category;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    Optional<Category> findByName(String name);
    
    boolean existsByName(String name);

    // Case-insensitive exists check
    boolean existsByNameIgnoreCase(String name);
    
    Page<Category> findByActive(Boolean active, Pageable pageable);
    
    @Query("SELECT c FROM Category c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Category> searchCategories(@Param("keyword") String keyword, Pageable pageable);

    // ==================== NEW METHODS FOR HIERARCHICAL SUPPORT
    // ====================

    /**
     * Finds a category by name and parent ID for hierarchical uniqueness.
     * Allows same category name under different parents.
     *
     * @param name     the category name
     * @param parentId the parent category ID (null for root categories)
     * @return Optional containing the category if found
     */
    @Query("""
            SELECT c FROM Category c
            WHERE c.name = :name
            AND (:parentId IS NULL AND c.parent IS NULL
                 OR c.parent.id = :parentId)
            """)
    Optional<Category> findByNameAndParentId(
            @Param("name") String name,
            @Param("parentId") Long parentId);

    /**
     * Batch lookup for categories by names.
     *
     * @param names collection of category names
     * @return list of categories matching the names
     */
    @Query("SELECT c FROM Category c WHERE c.name IN :names")
    List<Category> findAllByNameIn(@Param("names") Collection<String> names);

    /**
     * Gets only the names of existing categories for existence check.
     *
     * @param names collection of names to check
     * @return set of names that exist in the database
     */
    @Query("SELECT c.name FROM Category c WHERE c.name IN :names")
    Set<String> findExistingNames(@Param("names") Collection<String> names);

    /**
     * Finds category by SEO-friendly slug.
     *
     * @param slug the category slug
     * @return Optional containing the category if found
     */
    Optional<Category> findBySlug(String slug);

    /**
     * Finds all root categories (no parent).
     *
     * @return list of root categories
     */
    List<Category> findByParentIsNull();

    /**
     * Finds all root categories with pagination.
     *
     * @param pageable pagination information
     * @return page of root categories
     */
    Page<Category> findByParentIsNull(Pageable pageable);

    /**
     * Finds all active root categories.
     *
     * @return list of active root categories
     */
    List<Category> findByParentIsNullAndActiveTrue();

    /**
     * Finds children of a specific parent category.
     *
     * @param parentId the parent category ID
     * @return list of child categories
     */
    List<Category> findByParentId(Long parentId);

    /**
     * Checks if any categories exist with pessimistic write lock.
     * Used for race condition protection during seeding.
     *
     * @return true if any categories exist
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Category c")
    boolean existsAnyCategoryWithLock();
}
