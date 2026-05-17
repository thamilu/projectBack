package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.core.util.SecurityUtils;

import com.eshop.app.catalog.api.request.ProductReviewRequest;
import com.eshop.app.catalog.api.response.ProductReviewResponse;
import com.eshop.app.catalog.application.mapper.ProductReviewMapper;
import com.eshop.app.catalog.application.port.in.ProductReviewUseCase;
import com.eshop.app.catalog.domain.entity.Product;
import com.eshop.app.catalog.domain.entity.ProductReview;
import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.catalog.domain.repository.ProductReviewRepository;
import com.eshop.app.order.domain.repository.OrderRepository;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ConflictException;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductReviewUseCaseImpl implements ProductReviewUseCase {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewMapper reviewMapper;

    @Override
    public ProductReviewResponse createReview(ProductReviewRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
            .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), currentUserId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean hasPurchased = orderRepository.existsByUserIdAndOrderItemsProductId(currentUserId, request.getProductId());

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .comment(request.getComment())
                .verifiedPurchase(hasPurchased)
                .active(true)
                .build();

        ProductReview savedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(savedReview);
    }

    @Override
    public ProductReviewResponse updateReview(Long reviewId, ProductReviewRequest request) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
            .orElseThrow(() -> new IllegalStateException("User not authenticated"));
        if (!review.getUser().getId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
            throw new AccessDeniedException("You can only update your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ProductReview updatedReview = reviewRepository.save(review);
        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    public void deleteReview(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
            .orElseThrow(() -> new IllegalStateException("User not authenticated"));
        if (!review.getUser().getId().equals(currentUserId) && !SecurityUtils.hasRole("ADMIN")) {
            throw new AccessDeniedException("You can only delete your own reviews");
        }

        review.setActive(false);
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getReviewById(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + reviewId));

        if (!review.getActive()) {
            throw new ResourceNotFoundException("Review not found with id: " + reviewId);
        }

        return reviewMapper.toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getReviewsByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        Page<ProductReview> reviews = reviewRepository.findByProductIdAndActiveTrue(productId, pageable);
        return PageResponse.of(reviews, reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getReviewsByUser(Long userId, Pageable pageable) {
        Page<ProductReview> reviews = reviewRepository.findByUserIdAndActiveTrue(userId, pageable);
        return PageResponse.of(reviews, reviewMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getCurrentUserReviews(Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
            .orElseThrow(() -> new IllegalStateException("User not authenticated"));
        Page<ProductReview> reviews = reviewRepository.findByUserIdAndActiveTrue(currentUserId, pageable);
        return PageResponse.of(reviews, reviewMapper::toResponse);
    }

    @Override
    public boolean hasUserReviewedProduct(Long productId, Long userId) {
        return reviewRepository.existsByProductIdAndUserId(productId, userId);
    }
}


