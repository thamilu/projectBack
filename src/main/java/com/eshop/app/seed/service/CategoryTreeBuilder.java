package com.eshop.app.seed.service;

import com.eshop.app.entity.Category;
import com.eshop.app.seed.model.CategoryNode;
import com.github.slugify.Slugify;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for building Category entities from CategoryNode definitions.
 * 
 * <p>This service transforms the lightweight CategoryNode tree structure into
 * fully-formed JPA Category entities with:
 * <ul>
 *   <li>Unique SEO-friendly slugs</li>
 *   <li>Materialized paths for efficient queries</li>
 *   <li>Depth tracking for hierarchy level</li>
 *   <li>Display order for sorting</li>
 * </ul>
 *
 * <p><b>Slug Generation Strategy:</b>
 * <ul>
 *   <li>Root category: {@code fashion-apparel}</li>
 *   <li>Child category: {@code fashion-apparel-men}</li>
 *   <li>Grandchild: {@code fashion-apparel-men-t-shirts}</li>
 * </ul>
 *
 * <p><b>Path Generation:</b>
 * <ul>
 *   <li>Root: {@code "Electronics"}</li>
 *   <li>Child: {@code "Electronics > Mobiles"}</li>
 *   <li>Grandchild: {@code "Electronics > Mobiles > Smartphones"}</li>
 * </ul>
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryTreeBuilder {

    private final Slugify slugify = Slugify.builder().build();

    /**
     * Builds a complete list of Category entities from root CategoryNode list.
     * 
     * <p>This method flattens the tree structure into a linear list while
     * preserving parent-child relationships via the parent field.
     *
     * @param nodes the list of root category nodes
     * @return a flat list of all Category entities in the tree
     */
    public List<Category> buildTree(List<CategoryNode> nodes) {
        log.debug("Building category tree from {} root nodes", nodes.size());
        
        List<Category> result = new ArrayList<>();
        AtomicInteger orderCounter = new AtomicInteger(0);
        
        for (CategoryNode node : nodes) {
            buildCategoryRecursive(node, null, result, "", 0, orderCounter);
        }
        
        log.info("Built category tree: {} total categories", result.size());
        return result;
    }

    /**
     * Recursively builds Category entities from a CategoryNode and its descendants.
     *
     * @param node the current category node
     * @param parent the parent Category entity (null for root)
     * @param accumulator the list to accumulate all Category entities
     * @param pathPrefix the accumulated path of ancestor categories
     * @param depth the current depth in the hierarchy (0 = root)
     * @param orderCounter atomic counter for display order
     */
    private void buildCategoryRecursive(
            CategoryNode node,
            Category parent,
            List<Category> accumulator,
            String pathPrefix,
            int depth,
            AtomicInteger orderCounter) {

        // Generate materialized path
        String currentPath = pathPrefix.isEmpty()
            ? node.name()
            : pathPrefix + " > " + node.name();

        // Generate unique slug
        String slug = generateUniqueSlug(node.name(), parent);

        // Build Category entity
        Category category = Category.builder()
            .name(node.name())
            .description(node.name())  // Default description to name
            .slug(slug)
            .path(currentPath)
            .depth(depth)
            .displayOrder(orderCounter.getAndIncrement())
            .active(true)
            .parent(parent)
            .build();

        accumulator.add(category);

        log.trace("Built category: {} (slug: {}, depth: {}, path: {})",
                 node.name(), slug, depth, currentPath);

        // Recursively process children
        for (CategoryNode child : node.children()) {
            buildCategoryRecursive(
                child,
                category,
                accumulator,
                currentPath,
                depth + 1,
                orderCounter
            );
        }
    }

    /**
     * Generates a unique SEO-friendly slug for a category.
     * 
     * <p>Slugs are hierarchical: children include parent slugs as prefix.
     * 
     * <p>Examples:
     * <ul>
     *   <li>{@code "Fashion & Apparel"} → {@code "fashion-apparel"}</li>
     *   <li>{@code "Men"} (under Fashion) → {@code "fashion-apparel-men"}</li>
     *   <li>{@code "T-Shirts"} (under Men) → {@code "fashion-apparel-men-t-shirts"}</li>
     * </ul>
     *
     * @param name the category name
     * @param parent the parent category (null for root)
     * @return the generated slug
     */
    private String generateUniqueSlug(String name, Category parent) {
        String baseSlug = slugify.slugify(name);
        
        if (parent != null && parent.getSlug() != null) {
            return parent.getSlug() + "-" + baseSlug;
        }
        
        return baseSlug;
    }
}
