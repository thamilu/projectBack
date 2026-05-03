package com.eshop.app.service.impl;

import com.eshop.app.dto.request.ProductReviewRequest;
import com.eshop.app.dto.response.PageResponse;
import com.eshop.app.dto.response.ProductReviewResponse;
import com.eshop.app.entity.Product;
import com.eshop.app.entity.ProductReview;
import com.eshop.app.entity.User;
import com.eshop.app.exception.ConflictException;
import com.eshop.app.exception.ResourceNotFoundException;
import com.eshop.app.mapper.ProductReviewMapper;
import com.eshop.app.repository.OrderRepository;
import com.eshop.app.repository.ProductRepository;
import com.eshop.app.repository.ProductReviewRepository;
import com.eshop.app.repository.UserRepository;
import com.eshop.app.service.ProductReviewService;
import com.eshop.app.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductReviewMapper reviewMapper;

    @Override
    public ProductReviewResponse createReview(ProductReviewRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId().map(Long::parseLong)
            .orElseThrow(() -> new IllegalStateException("User not authenticated"));

        // Check if product exists
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // Check if user already reviewed this product
        if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), currentUserId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Check if user has purchased this product (for verified purchase)
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

        review.setActive(false); // Soft delete
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
        // Verify product exists
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