package com.eshop.app.catalog.infrastructure.security;

import com.eshop.app.catalog.domain.repository.ProductRepository;
import com.eshop.app.core.infrastructure.config.security.oauth.PrincipalDetails;

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

    /**
     * SpEL entry point for {@code @PreAuthorize("... @productSecurityService.isOwner(#id, principal)")}.
     * Resolves the authenticated seller's local user id from the JWT-derived principal.
     *
     * @param productId the product ID
     * @param principal the authenticated principal from the SpEL evaluation context
     * @return true if the principal is a resolved {@link PrincipalDetails} owning the product
     */
    public boolean isOwner(Long productId, Object principal) {
        if (!(principal instanceof PrincipalDetails principalDetails)) {
            return false;
        }
        return isProductOwner(productId, principalDetails.getId());
    }
}
