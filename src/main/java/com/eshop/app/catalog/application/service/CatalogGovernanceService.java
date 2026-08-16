package com.eshop.app.catalog.application.service;

import com.eshop.app.catalog.domain.entity.MasterProduct;

/**
 * Service for catalog duplicate checking and Levenshtein name similarity calculation. Extracted to
 * promote method reusability and clean architecture.
 */
public interface CatalogGovernanceService {

    /**
     * Calculate name similarity percentage using Levenshtein distance.
     *
     * @param s1 first string
     * @param s2 second string
     * @return similarity score between 0.0 and 1.0
     */
    double calculateSimilarity(String s1, String s2);

    /**
     * Check if a product has an exact match or similar master templates in the catalog.
     *
     * @param name product name
     * @param categoryId product category ID
     * @param brandId product brand ID (optional)
     * @return duplicate screening result
     */
    CatalogCheckResult checkDuplicates(String name, Long categoryId, Long brandId);

    /** DTO containing duplicate check results. */
    class CatalogCheckResult {
        private final MasterProduct exactMatch;
        private final MasterProduct similarMatch;
        private final double similarityScore;

        private CatalogCheckResult(
                MasterProduct exactMatch, MasterProduct similarMatch, double similarityScore) {
            this.exactMatch = exactMatch;
            this.similarMatch = similarMatch;
            this.similarityScore = similarityScore;
        }

        public static CatalogCheckResult exactMatch(MasterProduct exactMatch) {
            return new CatalogCheckResult(exactMatch, null, 1.0);
        }

        public static CatalogCheckResult similarMatch(
                MasterProduct similarMatch, double similarityScore) {
            return new CatalogCheckResult(null, similarMatch, similarityScore);
        }

        public static CatalogCheckResult none() {
            return new CatalogCheckResult(null, null, 0.0);
        }

        public boolean hasExactMatch() {
            return exactMatch != null;
        }

        public boolean hasSimilarMatch() {
            return similarMatch != null;
        }

        public MasterProduct getExactMatch() {
            return exactMatch;
        }

        public MasterProduct getSimilarMatch() {
            return similarMatch;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }
    }
}
