package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.api.response.ProductDuplicateCandidateResponse;
import com.eshop.app.catalog.application.mapper.ProductDuplicateCandidateMapper;
import com.eshop.app.catalog.application.port.in.CatalogMergeUseCase;
import com.eshop.app.catalog.domain.entity.MasterProduct;
import com.eshop.app.catalog.domain.entity.ProductDuplicateCandidate;
import com.eshop.app.catalog.domain.entity.ProductMedia;
import com.eshop.app.catalog.domain.repository.MasterProductRepository;
import com.eshop.app.catalog.domain.repository.ProductDuplicateCandidateRepository;
import com.eshop.app.core.api.response.PageResponse;
import com.eshop.app.core.exception.business.ResourceNotFoundException;
import static com.eshop.app.core.infrastructure.config.security.SecurityExpressions.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for CatalogMergeUseCase providing transactional merging, relinking, and
 * dismissal of duplicate master catalog items.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(rollbackFor = Exception.class)
public class CatalogMergeService implements CatalogMergeUseCase {

    private final MasterProductRepository masterProductRepository;
    private final ProductDuplicateCandidateRepository candidateRepository;
    private final ProductDuplicateCandidateMapper duplicateCandidateMapper;

    @PersistenceContext private EntityManager entityManager;

    @Override
    @PreAuthorize(IS_ADMIN)
    public void mergeMasterProducts(Long sourceProductId, Long targetProductId) {
        log.info(
                "Starting catalog merge: Source master ID {} -> Target master ID {}",
                sourceProductId,
                targetProductId);

        if (sourceProductId.equals(targetProductId)) {
            throw new IllegalArgumentException("Cannot merge a product into itself.");
        }

        MasterProduct source =
                masterProductRepository
                        .findById(sourceProductId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Source master product not found: "
                                                        + sourceProductId));

        MasterProduct target =
                masterProductRepository
                        .findById(targetProductId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Target master product not found: "
                                                        + targetProductId));

        // 1. Relink all seller listings in DB to point to target
        log.info(
                "Relinking seller products from master ID {} to target ID {}",
                sourceProductId,
                targetProductId);
        int updatedListingsCount =
                entityManager
                        .createQuery(
                                "UPDATE Product p SET p.masterProduct = :target WHERE"
                                        + " p.masterProduct = :source")
                        .setParameter("target", target)
                        .setParameter("source", source)
                        .executeUpdate();
        log.info("Successfully relinked {} seller product listings.", updatedListingsCount);

        // 2. Transfer/Merge media assets (ProductMedia) from source to target
        log.info("Transferring catalog media assets from source to target");
        if (source.getMedia() != null) {
            for (ProductMedia sm : source.getMedia()) {
                // Check if target already has this URL to avoid duplicates
                boolean duplicateMedia =
                        target.getMedia().stream()
                                .anyMatch(
                                        tm -> tm.getMediaUrl().equalsIgnoreCase(sm.getMediaUrl()));
                if (!duplicateMedia) {
                    ProductMedia newMedia =
                            ProductMedia.builder()
                                    .masterProduct(target)
                                    .mediaUrl(sm.getMediaUrl())
                                    .mediaType(sm.getMediaType())
                                    .isPrimary(false) // Target primary takes precedence
                                    .sortOrder(sm.getSortOrder() + 10)
                                    .altText(sm.getAltText())
                                    .build();
                    target.getMedia().add(newMedia);
                }
            }
            masterProductRepository.save(target);
        }

        // 3. Mark source master product as resolved/merged & deactivated
        source.setActive(false);
        source.setDuplicateStatus("RESOLVED_MERGED");
        source.setMergedIntoProductId(targetProductId);
        masterProductRepository.save(source);

        // 4. Update matching candidates review status
        List<ProductDuplicateCandidate> candidates =
                candidateRepository.findBySourceProductIdOrMatchedProductId(
                        sourceProductId, targetProductId);
        for (ProductDuplicateCandidate c : candidates) {
            c.setReviewStatus("MERGED");
            candidateRepository.save(c);
        }

        log.info(
                "Catalog merge completed successfully: Source master ID {} deactivated and merged"
                        + " into ID {}",
                sourceProductId,
                targetProductId);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize(IS_ADMIN)
    public PageResponse<ProductDuplicateCandidateResponse> getDuplicateCandidates(Pageable pageable) {
        return PageResponse.of(
                candidateRepository.findByReviewStatus("PENDING", pageable),
                duplicateCandidateMapper::toResponse);
    }

    @Override
    @PreAuthorize(IS_ADMIN)
    public void dismissCandidate(Long candidateId) {
        log.info("Dismissing duplicate catalog review candidate: ID={}", candidateId);
        ProductDuplicateCandidate candidate =
                candidateRepository
                        .findById(candidateId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Duplicate candidate not found with id: "
                                                        + candidateId));
        candidate.setReviewStatus("DISMISSED");
        candidateRepository.save(candidate);

        // Update the source master product to ACTIVE standard APPROVED state
        MasterProduct source =
                masterProductRepository.findById(candidate.getSourceProductId()).orElse(null);
        if (source != null) {
            source.setDuplicateStatus("NONE");
            masterProductRepository.save(source);
        }
    }
}
