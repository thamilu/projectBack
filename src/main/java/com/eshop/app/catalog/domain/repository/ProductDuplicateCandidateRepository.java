package com.eshop.app.catalog.domain.repository;

import com.eshop.app.catalog.domain.entity.ProductDuplicateCandidate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA Repository interface for ProductDuplicateCandidate audit entities. */
@Repository
public interface ProductDuplicateCandidateRepository
        extends JpaRepository<ProductDuplicateCandidate, Long> {

    Page<ProductDuplicateCandidate> findByReviewStatus(String reviewStatus, Pageable pageable);

    List<ProductDuplicateCandidate> findBySourceProductIdOrMatchedProductId(
            Long sourceProductId, Long matchedProductId);
}
