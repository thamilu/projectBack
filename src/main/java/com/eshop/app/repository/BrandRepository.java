package com.eshop.app.repository;

import com.eshop.app.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import java.util.Objects;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {
    
    Optional<Brand> findByName(String name);
    
    boolean existsByName(String name);

    boolean existsByNameIgnoreCase(String name);
    
    Optional<Brand> findBySlug(String slug);

    Optional<Brand> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    
    Page<Brand> findByActive(Boolean active, Pageable pageable);

    Page<Brand> findByActiveTrue(Pageable pageable);

    @Query("SELECT b FROM Brand b JOIN b.products p WHERE p.category.id = :categoryId")
    Page<Brand> findByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Brand> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE b.featured = true ORDER BY b.createdAt DESC")
    Page<Brand> findFeaturedBrands(Pageable pageable);

    @Query("SELECT b.name FROM Brand b WHERE LOWER(b.name) IN :names")
    List<String> findNamesLowerCaseIn(@Param("names") List<String> names);

    default List<String> findExistingNames(List<String> names) {
        if (names == null || names.isEmpty()) return List.of();
        List<String> lower = names.stream().map(s -> s == null ? null : s.toLowerCase()).filter(Objects::nonNull).toList();
        return findNamesLowerCaseIn(lower);
    }

    @Query("SELECT new com.eshop.app.dto.response.BrandSummaryResponse(b.id, b.name, b.slug, b.logoUrl) FROM Brand b WHERE b.active = true ORDER BY b.name")
    List<com.eshop.app.dto.response.BrandSummaryResponse> findAllActiveBrandsSummary();

    @Query("SELECT b.name FROM Brand b WHERE LOWER(b.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY b.name")
    List<String> findBrandNamesByPrefix(@Param("prefix") String prefix, Pageable pageable);

    default Page<Brand> searchWithCriteria(com.eshop.app.dto.request.BrandSearchCriteria criteria, Pageable pageable) {
        if (criteria == null) return findAll(pageable);
        org.springframework.util.StringUtils.hasText(""); // ensure import usage
        if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
            return searchByKeyword(criteria.getKeyword(), pageable);
        }
        if (criteria.getActive() != null) {
            return findByActive(criteria.getActive(), pageable);
        }
        return findAll(pageable);
    }

    default Optional<Brand> findByIdIncludeDeleted(Long id) {
        return findById(id);
    }

    long countByActiveTrue();
    
    @Query("SELECT b FROM Brand b WHERE " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(b.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Brand> searchBrands(@Param("keyword") String keyword, Pageable pageable);
}
