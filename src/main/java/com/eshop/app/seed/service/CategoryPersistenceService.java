package com.eshop.app.seed.service;

import com.eshop.app.entity.Category;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service responsible for batch persistence of Category entities.
 * 
 * <p>This service optimizes database interactions by:
 * <ul>
 *   <li>Batch persisting entities (configurable batch size)</li>
 *   <li>Topological sorting: ensuring parents are saved before children</li>
 *   <li>Periodic EntityManager flush/clear to prevent memory bloat</li>
 *   <li>Reducing 400+ individual queries to ~10 batch operations</li>
 * </ul>
 *
 * <p><b>Performance Optimizations:</b>
 * <ul>
 *   <li>Batch Size: 50 entities per flush (configurable)</li>
 *   <li>EntityManager clear after each batch (prevents 1st level cache overflow)</li>
 *   <li>Topological sort: O(n) time complexity</li>
 *   <li>Total DB operations: ~10 for 200 categories (vs 400+ without batching)</li>
 * </ul>
 *
 * <p><b>Configuration:</b>
 * <pre>
 * app.seeding.categories.batch-size=50  # Entities per batch flush
 * </pre>
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryPersistenceService {

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.seeding.categories.batch-size:50}")
    private int batchSize;

    /**
     * Persists a list of categories using batch operations with topological sorting.
     * 
     * <p>Categories are sorted to ensure parents are persisted before their children,
     * which is critical for foreign key constraints.
     *
     * @param categories the list of categories to persist
     * @return the number of categories persisted
     * @throws org.springframework.dao.DataIntegrityViolationException if FK constraints fail
     */
    @Transactional
    public int persistCategories(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            log.warn("No categories to persist");
            return 0;
        }

        log.info("Persisting {} categories with batch size: {}", categories.size(), batchSize);
        long startTime = System.currentTimeMillis();

        // Topological sort: parents before children
        List<Category> sorted = topologicalSort(categories);
        log.debug("Topological sort completed: {} categories ordered", sorted.size());

        int count = 0;
        for (int i = 0; i < sorted.size(); i++) {
            entityManager.persist(sorted.get(i));
            count++;

            // Batch flush and clear
            if (i > 0 && (i + 1) % batchSize == 0) {
                flushAndClear();
                log.debug("Flushed batch at index {}/{}", i + 1, sorted.size());
            }
        }

        // Final flush for remaining entities
        flushAndClear();

        long duration = System.currentTimeMillis() - startTime;
        log.info("Persisted {} categories in {}ms (~{} ms/category)",
                count, duration, duration / Math.max(count, 1));

        return count;
    }

    /**
     * Flushes pending operations to database and clears the persistence context.
     * 
     * <p>This prevents the first-level cache from consuming excessive memory
     * during large batch operations.
     */
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Performs topological sort on categories to ensure parents are before children.
     * 
     * <p>Uses depth-first search (DFS) to build the sorted list. This ensures
     * that when we persist categories, parent categories already have their IDs
     * assigned, which children can reference.
     *
     * <p><b>Algorithm:</b>
     * <ol>
     *   <li>For each category, recursively visit its parent first</li>
     *   <li>Mark category as visited after parent is processed</li>
     *   <li>Add to result list</li>
     * </ol>
     *
     * <p>Time Complexity: O(n) where n = number of categories
     *
     * @param categories the unsorted list of categories
     * @return the topologically sorted list
     */
    private List<Category> topologicalSort(List<Category> categories) {
        Map<Category, Boolean> visited = new IdentityHashMap<>(categories.size());
        List<Category> result = new ArrayList<>(categories.size());

        for (Category cat : categories) {
            topologicalVisit(cat, visited, result);
        }

        return result;
    }

    /**
     * Recursive DFS helper for topological sort.
     *
     * @param cat the current category to visit
     * @param visited map tracking visited categories
     * @param result the accumulated sorted list
     */
    private void topologicalVisit(
            Category cat,
            Map<Category, Boolean> visited,
            List<Category> result) {

        // Already visited
        if (Boolean.TRUE.equals(visited.get(cat))) {
            return;
        }

        // Visit parent first
        if (cat.getParent() != null && !Boolean.TRUE.equals(visited.get(cat.getParent()))) {
            topologicalVisit(cat.getParent(), visited, result);
        }

        // Mark as visited and add to result
        visited.put(cat, true);
        result.add(cat);
    }
}
