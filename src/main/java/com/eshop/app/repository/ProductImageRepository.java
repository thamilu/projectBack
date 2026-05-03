package com.eshop.app.repository;

import com.eshop.app.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    
    List<ProductImage> findByProductIdAndActiveTrue(Long productId);
    
    List<ProductImage> findByProductIdAndActiveTrueOrderByDisplayOrderAsc(Long productId);
    
    Optional<ProductImage> findByProductIdAndIsPrimaryTrueAndActiveTrue(Long productId);
    
    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId AND pi.isPrimary = true AND pi.active = true")
    Optional<ProductImage> findPrimaryImageByProductId(@Param("productId") Long productId);
    
    boolean existsByProductIdAndIsPrimaryTrueAndActiveTrue(Long productId);
}