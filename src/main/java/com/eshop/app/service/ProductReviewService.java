package com.eshop.app.service;

import com.eshop.app.dto.request.ProductReviewRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {
    
    ProductReviewResponse createReview(ProductReviewRequest request);
    
    ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request);
    
    void deleteReview(Long reviewId);
    
    ProductReviewResponse getReviewById(Long reviewId);
    
    PageResponse<ProductReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
    
    PageResponse<ProductReviewResponse> getReviewsByUser(Long userId, Pageable pageable);
    
    PageResponse<ProductReviewResponse> getCurrentUserReviews(Pageable pageable);
    
    boolean hasUserReviewedProduct(Long productId, Long userId);
}