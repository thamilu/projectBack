package com.eshop.app.repository;

import com.eshop.app.entity.CategoryCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryCommissionRepository extends JpaRepository<CategoryCommission, Long> {
    
    Optional<CategoryCommission> findByCategoryId(Long categoryId);
    
    // Find active commission for a category
    Optional<CategoryCommission> findByCategoryIdAndActiveTrue(Long categoryId);
}
