package com.eshop.app.seller.application.port.in;

/**
 * Inbound port defining the use case contract for synchronising a seller's platform role with an
 * external identity provider (e.g., Keycloak).
 *
 * <p>Role synchronisation ensures that an approved seller's granted authorities are reflected in
 * the authentication system. This operation is separated from the approval workflow use case
 * because it is an infrastructure synchronisation concern that may be triggered by multiple events
 * (approval, manual admin action, scheduled reconciliation job) and must be independently mockable
 * and testable.
 *
 * <h3>Responsibility</h3>
 *
 * <p>Assigns or updates the {@code ROLE_SELLER} authority for the user associated with the given
 * seller profile in the external identity provider.
 *
 * <h3>Idempotency</h3>
 *
 * <p>This operation <strong>should be idempotent</strong>. Calling it for a seller whose role is
 * already synchronised must not produce an error or duplicate role assignment. Implementors must
 * ensure the external identity provider call is idempotent (upsert semantics).
 *
 * <h3>Failure Handling</h3>
 *
 * <p>If the external identity provider is unavailable, implementors must throw a domain exception
 * rather than silently failing. The caller is responsible for retry strategy (e.g., via a scheduled
 * job or message queue).
 *
 * <h3>Transaction</h3>
 *
 * <p>This operation interacts with an external system and must not be wrapped in a database
 * transaction that spans the external call. Implementors must coordinate local state updates and
 * external calls appropriately (e.g., outbox pattern).
 *
 * @see SellerApprovalUseCase
 */
public interface SellerRoleSyncUseCase {

    /**
     * Synchronises the platform role of the seller identified by {@code sellerId} with the external
     * identity provider.
     *
     * <h4>Preconditions</h4>
     *
     * <ul>
     *   <li>{@code sellerId} must not be {@code null} and must identify an existing, approved
     *       seller profile.
     * </ul>
     *
     * <h4>Postconditions</h4>
     *
     * <ul>
     *   <li>The seller's associated user holds the {@code ROLE_SELLER} authority in the external
     *       identity provider.
     *   <li>The local seller profile's role-sync status is updated.
     * </ul>
     *
     * <h4>Idempotency</h4>
     *
     * <p>Safe to call multiple times for the same {@code sellerId}. Duplicate calls must not result
     * in duplicate role assignments or errors.
     *
     * @param sellerId the ID of the seller whose role is to be synchronised; must not be {@code
     *     null}
     * @throws com.eshop.app.core.exception.business.ResourceNotFoundException if no seller profile
     *     exists for {@code sellerId}
     * @throws com.eshop.app.core.exception.business.ValidationException if the external identity
     *     provider is unavailable or returns an error
     * @throws IllegalArgumentException if {@code sellerId} is {@code null}
     */
    void syncSellerRole(Long sellerId);
}
