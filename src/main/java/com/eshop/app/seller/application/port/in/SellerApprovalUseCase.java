package com.eshop.app.seller.application.port.in;

import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.user.api.response.SellerProfileResponse;
import org.springframework.data.domain.Pageable;

/**
 * Inbound port defining the use case contract for seller approval workflow operations.
 *
 * <p>This interface forms the boundary between the admin driving adapter (REST controller) and the
 * application core for approval lifecycle management. It is implemented by the seller
 * administration service and must contain no infrastructure dependencies.
 *
 * <h3>Responsibility</h3>
 *
 * <p>Covers all operations related to reviewing, approving, and rejecting seller registration
 * applications. This interface is intentionally limited to approval workflow concerns; reference
 * data and role synchronisation are defined in separate use case ports per the Interface
 * Segregation Principle.
 *
 * <h3>Security</h3>
 *
 * <p>All methods in this interface are restricted to users holding the {@code ROLE_ADMIN} or {@code
 * ROLE_OPERATIONS} authority. Enforcement is the responsibility of the security layer in the
 * driving adapter.
 *
 * <h3>Transaction</h3>
 *
 * <p>Implementors are responsible for defining appropriate transaction boundaries. State-mutating
 * commands ({@link #approveSeller}, {@link #rejectSeller}) must execute within a write transaction.
 * The query method ({@link #getPendingSellers}) should use a read-only transaction.
 *
 * @see SellerReferenceDataUseCase
 * @see SellerRoleSyncUseCase
 */
public interface SellerApprovalUseCase {

    /**
     * Retrieves a paginated list of seller profiles awaiting approval.
     *
     * <h4>Preconditions</h4>
     *
     * <ul>
     *   <li>{@code pageable} must not be {@code null}.
     * </ul>
     *
     * <h4>Postconditions</h4>
     *
     * <ul>
     *   <li>Returns only sellers in {@code PENDING} status.
     *   <li>Result set is ordered by registration date, oldest first, to ensure fair FIFO
     *       processing of applications.
     *   <li>Returns an empty page — never {@code null} — when no pending sellers exist.
     * </ul>
     *
     * @param pageable pagination and sorting parameters; must not be {@code null}
     * @return a page of pending seller profiles; never {@code null}
     * @throws IllegalArgumentException if {@code pageable} is {@code null}
     */
    PageResponse<SellerProfileResponse> getPendingSellers(Pageable pageable);

    /**
     * Approves a seller's registration application, transitioning the seller profile to {@code
     * APPROVED} status.
     *
     * <h4>Preconditions</h4>
     *
     * <ul>
     *   <li>{@code sellerId} must not be {@code null} and must identify an existing seller profile.
     *   <li>{@code processedBy} must not be {@code null} or blank; it must identify the
     *       administrator performing the action (username or ID).
     *   <li>The seller must be in {@code PENDING} or {@code UNDER_REVIEW} status. Approving an
     *       already-approved seller is a no-op or error per implementor policy; the exception
     *       contract below applies.
     * </ul>
     *
     * <h4>Postconditions</h4>
     *
     * <ul>
     *   <li>Seller status is set to {@code APPROVED}.
     *   <li>Audit fields are updated with {@code processedBy} and the approval timestamp.
     *   <li>Any downstream role assignment is triggered (see {@link SellerRoleSyncUseCase}).
     * </ul>
     *
     * <h4>Idempotency</h4>
     *
     * <p>This operation is <strong>not idempotent</strong>. Approving an already-approved seller
     * must throw {@code ConflictException}.
     *
     * @param sellerId the ID of the seller to approve; must not be {@code null}
     * @param processedBy the identifier of the administrator approving the seller; must not be
     *     {@code null} or blank
     * @throws com.eshop.app.core.exception.business.ResourceNotFoundException if no seller profile
     *     exists for {@code sellerId}
     * @throws com.eshop.app.core.exception.business.ValidationException if the seller is already in
     *     {@code APPROVED} status or is not in PENDING status
     * @throws IllegalArgumentException if {@code sellerId} or {@code processedBy} is {@code null}
     *     or blank
     */
    void approveSeller(Long sellerId, String processedBy);

    /**
     * Rejects a seller's registration application, transitioning the seller profile to {@code
     * REJECTED} status.
     *
     * <h4>Preconditions</h4>
     *
     * <ul>
     *   <li>{@code sellerId} must not be {@code null} and must identify an existing seller profile.
     *   <li>{@code rejectionReason} must not be {@code null} or blank; it is persisted against the
     *       seller profile and may be communicated to the seller.
     *   <li>{@code processedBy} must not be {@code null} or blank; it must identify the
     *       administrator performing the action.
     *   <li>The seller must be in {@code PENDING} or {@code UNDER_REVIEW} status.
     * </ul>
     *
     * <h4>Postconditions</h4>
     *
     * <ul>
     *   <li>Seller status is set to {@code REJECTED}.
     *   <li>{@code rejectionReason} is persisted against the seller profile.
     *   <li>Audit fields are updated with {@code processedBy} and the rejection timestamp.
     * </ul>
     *
     * <h4>Idempotency</h4>
     *
     * <p>This operation is <strong>not idempotent</strong>. Rejecting an already-rejected seller
     * must throw {@code ConflictException}.
     *
     * @param sellerId the ID of the seller to reject; must not be {@code null}
     * @param rejectionReason the reason for rejection; must not be {@code null} or blank; maximum
     *     length is defined by the domain constraint on {@code SellerProfile.rejectionReason}
     * @param processedBy the identifier of the administrator rejecting the seller; must not be
     *     {@code null} or blank
     * @throws com.eshop.app.core.exception.business.ResourceNotFoundException if no seller profile
     *     exists for {@code sellerId}
     * @throws com.eshop.app.core.exception.business.ValidationException if the seller is already in
     *     {@code REJECTED} status or is not in PENDING status
     * @throws IllegalArgumentException if any parameter is {@code null} or blank
     */
    void rejectSeller(Long sellerId, String rejectionReason, String processedBy);
}
