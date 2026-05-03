package com.eshop.app.security;

import com.eshop.app.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Security service for product ownership validation.
 * Used in @PreAuthorize expressions for RBAC.
 * 
 * @since 2.0
 */
@Service("productSecurityService")
@RequiredArgsConstructor
public class ProductSecurityService {

    private final ProductRepository productRepository;

    /**
     * Check if a seller owns a specific product.
     * Used in SpEL expressions for authorization.
     * 
     * @param productId the product ID
     * @param sellerId  the seller ID
     * @return true if seller owns the product
     */
    public boolean isProductOwner(Long productId, Long sellerId) {
        if (productId == null || sellerId == null) {
            return false;
        }
        return productRepository.existsByIdAndStoreSellerProfileUserId(productId, sellerId);
    }
}
