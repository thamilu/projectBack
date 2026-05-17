package com.eshop.app.catalog.domain.repository;

import com.eshop.app.catalog.domain.entity.CategoryAttributeMapping;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAttributeMappingRepository extends JpaRepository<CategoryAttributeMapping, Long> {
    
    List<CategoryAttributeMapping> findByCategoryIdOrderByDisplayOrderAsc(Long categoryId);
    
    Optional<CategoryAttributeMapping> findByCategoryIdAndAttributeDefinitionId(Long categoryId, Long attributeDefinitionId);
    
    void deleteByCategoryId(Long categoryId);
}
