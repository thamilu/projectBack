package com.eshop.app.catalog.application.port.in;

import com.eshop.app.catalog.api.response.ProductDuplicateCandidateResponse;
import com.eshop.app.core.api.response.PageResponse;
import org.springframework.data.domain.Pageable;

/** UseCase port interface defining catalog duplicate review and merge functionalities. */
public interface CatalogMergeUseCase {

    /**
     * Merge source product listings and media into the target master product, soft-deactivating the
     * source.
     */
    void mergeMasterProducts(Long sourceProductId, Long targetProductId);

    /** Get paginated list of active duplicate candidates pending review. */
    PageResponse<ProductDuplicateCandidateResponse> getDuplicateCandidates(Pageable pageable);

    /** Dismiss a flagged candidate review without merging. */
    void dismissCandidate(Long candidateId);
}
