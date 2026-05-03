package com.eshop.app.seed.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable record representing a node in the category hierarchy tree.
 * 
 * <p>This record is used during the seeding process to define the category structure
 * before converting to JPA entities. It provides a clean, null-safe representation
 * of the hierarchy.
 *
 * <p><b>Example Usage:</b>
 * <pre>
 * CategoryNode electronics = new CategoryNode("Electronics", List.of(
 *     new CategoryNode("Mobiles", List.of(
 *         new CategoryNode("Smartphones", List.of()),
 *         new CategoryNode("Feature Phones", List.of())
 *     )),
 *     new CategoryNode("Computers", List.of())
 * ));
 * </pre>
 *
 * <p><b>Null Safety:</b>
 * <ul>
 *   <li>Name cannot be null (throws NullPointerException)</li>
 *   <li>Children list is converted to immutable empty list if null</li>
 *   <li>Children list is made immutable via List.copyOf()</li>
 * </ul>
 *
 * @param name the category name (required, non-null)
 * @param children the list of child category nodes (automatically converted to immutable list)
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
public record CategoryNode(String name, List<CategoryNode> children) {

    /**
     * Compact constructor for validation and defensive copying.
     * 
     * <p>Ensures:
     * <ul>
     *   <li>Name is not null</li>
     *   <li>Children list is never null (defaults to empty list)</li>
     *   <li>Children list is immutable</li>
     * </ul>
     *
     * @throws NullPointerException if name is null
     */
    public CategoryNode {
        Objects.requireNonNull(name, "Category name cannot be null");
        children = children != null ? List.copyOf(children) : Collections.emptyList();
    }

    /**
     * Factory method to create a leaf node (no children).
     *
     * @param name the category name
     * @return a new CategoryNode with no children
     */
    public static CategoryNode leaf(String name) {
        return new CategoryNode(name, Collections.emptyList());
    }

    /**
     * Factory method to create a node with children.
     *
     * @param name the category name
     * @param children the child nodes
     * @return a new CategoryNode with the specified children
     */
    public static CategoryNode of(String name, CategoryNode... children) {
        return new CategoryNode(name, List.of(children));
    }

    /**
     * Factory method to create a node with children from string names.
     *
     * @param name the category name
     * @param childNames the names of child categories (converted to leaf nodes)
     * @return a new CategoryNode with leaf children
     */
    public static CategoryNode withChildren(String name, String... childNames) {
        List<CategoryNode> childNodes = childNames != null && childNames.length > 0
            ? java.util.Arrays.stream(childNames).map(CategoryNode::leaf).toList()
            : Collections.emptyList();
        return new CategoryNode(name, childNodes);
    }

    /**
     * Checks if this node is a leaf (has no children).
     *
     * @return true if this node has no children, false otherwise
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Gets the number of children.
     *
     * @return the child count
     */
    public int getChildCount() {
        return children.size();
    }

    /**
     * Recursively counts the total number of nodes in this subtree (including this node).
     *
     * @return the total node count
     */
    public int getTotalNodeCount() {
        return 1 + children.stream()
            .mapToInt(CategoryNode::getTotalNodeCount)
            .sum();
    }
}
