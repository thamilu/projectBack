package com.eshop.app.mapper;

import com.eshop.app.dto.response.ProductReviewResponse;
import com.eshop.app.entity.ProductReview;
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
                                : review.getUser().getUsername())
                .rating(review.getRating())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}