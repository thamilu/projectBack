package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.request.ProductReviewRequest;
import com.eshop.app.catalog.api.response.ProductReviewResponse;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;

/**
 * Inbound Port for Product Review Use Cases.
 */
public interface ProductReviewUseCase {
    ProductReviewResponse createReview(ProductReviewRequest request);
    ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request);
    void deleteReview(Long reviewId);
    ProductReviewResponse getReviewById(Long reviewId);
    PageResponse<ProductReviewResponse> getReviewsByProduct(Long productId, Pageable pageable);
    PageResponse<ProductReviewResponse> getReviewsByUser(Long userId, Pageable pageable);
    PageResponse<ProductReviewResponse> getCurrentUserReviews(Pageable pageable);
    boolean hasUserReviewedProduct(Long productId, Long userId);
}

