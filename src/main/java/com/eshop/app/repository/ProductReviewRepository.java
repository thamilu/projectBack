package com.eshop.app.repository;

import com.eshop.app.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    
    Page<ProductReview> findByProductIdAndActiveTrue(Long productId, Pageable pageable);
    
    Page<ProductReview> findByUserIdAndActiveTrue(Long userId, Pageable pageable);
    
    Optional<ProductReview> findByProductIdAndUserIdAndActiveTrue(Long productId, Long userId);
    
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    
    @Query("SELECT AVG(pr.rating) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.active = true")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
    
    @Query("SELECT COUNT(pr) FROM ProductReview pr WHERE pr.product.id = :productId AND pr.active = true")
    Long countByProductIdAndActiveTrue(@Param("productId") Long productId);
    
    @Query("SELECT pr FROM ProductReview pr WHERE pr.product.id = :productId AND pr.active = true ORDER BY pr.createdAt DESC")
    List<ProductReview> findTop10ByProductIdOrderByCreatedAtDesc(@Param("productId") Long productId);
}