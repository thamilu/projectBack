package com.eshop.app.seller.application.port.in;

import com.eshop.app.seller.api.response.BusinessTypeResponse;
import java.util.List;

/**
 * Inbound port defining the use case contract for seller reference data queries.
 *
 * <p>Reference data is read-only, low-change-frequency data used to populate UI form fields and
 * validate seller registration inputs. It is separated from approval and synchronisation use cases
 * per the Interface Segregation Principle, allowing controllers and services to depend only on the
 * contract they require.
 *
 * <h3>Caching</h3>
 *
 * <p>Implementors are strongly encouraged to cache the result of {@link #getBusinessTypes} using
 * Spring Cache (e.g., Caffeine or Redis), as this data changes infrequently and is called on every
 * seller registration form load. Cache eviction should be triggered on administrative updates to
 * business type configuration.
 *
 * <h3>Transaction</h3>
 *
 * <p>All methods in this interface are read-only. Implementors must use read-only transactions
 * ({@code @Transactional(readOnly = true)}).
 *
 * @see SellerApprovalUseCase
 * @see SellerRoleSyncUseCase
 */
public interface SellerReferenceDataUseCase {

    /**
     * Returns all available business types for seller registration.
     *
     * <h4>Preconditions</h4>
     *
     * <p>None — this is a pure reference data query with no input parameters.
     *
     * <h4>Postconditions</h4>
     *
     * <ul>
     *   <li>Returns a non-null, possibly empty list of business type descriptors.
     *   <li>Each entry is a fully populated {@link BusinessTypeResponse}.
     *   <li>The list is ordered by display order or alphabetically as configured.
     * </ul>
     *
     * <h4>Caching</h4>
     *
     * <p>Implementors should annotate this method with {@code @Cacheable} using the {@code
     * businessTypes} cache key. The cache should be invalidated when business type configuration
     * changes.
     *
     * @return an ordered, non-null list of available business types; returns an empty list if none
     *     are configured
     */
    List<BusinessTypeResponse> getBusinessTypes();
}
