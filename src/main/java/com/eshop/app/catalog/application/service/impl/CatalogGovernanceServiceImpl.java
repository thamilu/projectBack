package com.eshop.app.catalog.application.service.impl;

import com.eshop.app.catalog.application.service.CatalogGovernanceService;
import com.eshop.app.catalog.domain.entity.MasterProduct;
import com.eshop.app.catalog.domain.repository.MasterProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service implementation for Catalog duplicate checks using Levenshtein distance. */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogGovernanceServiceImpl implements CatalogGovernanceService {

    private final MasterProductRepository masterProductRepository;
    private static final double SIMILARITY_THRESHOLD = 0.75;

    @Override
    public double calculateSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        String longer = s1.toLowerCase(), shorter = s2.toLowerCase();
        if (s1.length() < s2.length()) {
            longer = s2.toLowerCase();
            shorter = s1.toLowerCase();
        }
        int longerLength = longer.length();
        if (longerLength == 0) return 1.0;

        int[] costs = new int[shorter.length() + 1];
        for (int i = 0; i <= shorter.length(); i++) {
            costs[i] = i;
        }
        for (int i = 1; i <= longer.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= shorter.length(); j++) {
                int cj =
                        Math.min(
                                1 + Math.min(costs[j], costs[j - 1]),
                                longer.charAt(i - 1) == shorter.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        int editDistance = costs[shorter.length()];
        return (longerLength - editDistance) / (double) longerLength;
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogCheckResult checkDuplicates(String name, Long categoryId, Long brandId) {
        List<MasterProduct> activeMatches =
                masterProductRepository.findAllActive(Pageable.unpaged()).getContent();

        MasterProduct duplicateMaster = null;
        MasterProduct similarMaster = null;
        double highestSimilarity = 0.0;

        for (MasterProduct mp : activeMatches) {
            boolean sameBrand =
                    (brandId == null && mp.getBrand() == null)
                            || (brandId != null
                                    && mp.getBrand() != null
                                    && mp.getBrand().getId().equals(brandId));

            // 1. Exact match check
            if (mp.getName().equalsIgnoreCase(name)
                    && mp.getCategory().getId().equals(categoryId)
                    && sameBrand) {
                duplicateMaster = mp;
                break;
            }

            // 2. Similarity match check
            if (mp.getCategory().getId().equals(categoryId)) {
                double similarity = calculateSimilarity(mp.getName(), name);
                if (similarity >= SIMILARITY_THRESHOLD && similarity > highestSimilarity) {
                    highestSimilarity = similarity;
                    similarMaster = mp;
                }
            }
        }

        if (duplicateMaster != null) {
            return CatalogCheckResult.exactMatch(duplicateMaster);
        } else if (similarMaster != null) {
            return CatalogCheckResult.similarMatch(similarMaster, highestSimilarity);
        }

        return CatalogCheckResult.none();
    }
}
