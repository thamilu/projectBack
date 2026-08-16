package com.eshop.app.user.application.port.in;

import com.eshop.app.user.api.request.SellerProfileUpdateRequest;
import com.eshop.app.user.api.response.SellerProfileResponse;
import org.springframework.security.core.Authentication;

/**
 * Use case port for seller profile self-service operations.
 *
 * <p>Defines the inbound port for authenticated sellers to retrieve, update, and check the
 * existence of their own seller profile.
 *
 * <p>This interface represents the boundary between the controller (adapter) layer and the
 * application (service) layer, following the Ports and Adapters (Hexagonal) architecture pattern.
 *
 * <p><strong>Implementations:</strong> {@code
 * com.eshop.app.user.application.service.SellerProfileService}
 */
public interface SellerProfileUseCase {

    /**
     * Retrieve the complete seller profile for the authenticated principal.
     *
     * @param authentication the Spring Security authentication context of the caller
     * @return the complete seller profile including KYC, bank accounts, and documents
     * @throws com.eshop.app.core.exception.business.ResourceNotFoundException if no seller profile
     *     exists for the authenticated user
     */
    SellerProfileResponse getSellerProfile(Authentication authentication);

    /**
     * Update the seller profile for the given user.
     *
     * @param userId the internal user ID of the authenticated seller
     * @param request the update request containing fields to modify
     * @return the updated seller profile
     * @throws com.eshop.app.core.exception.ResourceNotFoundException if no seller profile exists
     *     for the given user ID
     */
    SellerProfileResponse updateSellerProfile(Long userId, SellerProfileUpdateRequest request);

    /**
     * Check whether a seller profile exists for the authenticated principal.
     *
     * @param authentication the Spring Security authentication context of the caller
     * @return {@code true} if the user has completed seller registration; {@code false} otherwise
     */
    boolean hasProfile(Authentication authentication);
}
