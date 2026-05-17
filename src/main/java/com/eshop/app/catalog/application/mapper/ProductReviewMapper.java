package com.eshop.app.catalog.application.mapper;

import com.eshop.app.catalog.api.response.ProductReviewResponse;
import com.eshop.app.catalog.domain.entity.ProductReview;




import org.springframework.stereotype.Component;

@Component
public class ProductReviewMapper {
    
    public ProductReviewResponse toResponse(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(
                        review.getUser().getUserProfile() != null
                                ? review.getUser().getUserProfile().getFirstName() + " "
                                        + review.getUser().getUserProfile().getLastName()
                                : review.getUser().getEmail())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
