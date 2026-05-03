package com.eshop.app.seed.validation;

import com.eshop.app.exception.InvalidCategoryHierarchyException;
import com.eshop.app.seed.model.CategoryNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validator for category hierarchy structure and node properties.
 * 
 * <p>
 * Performs comprehensive validation including:
 * <ul>
 * <li>Maximum depth enforcement (prevents excessively deep hierarchies)</li>
 * <li>Name pattern validation (ensures valid characters)</li>
 * <li>Name length validation (2-100 characters)</li>
 * <li>Null safety checks</li>
 * </ul>
 *
 * <p>
 * <b>Configuration Properties:</b>
 * 
 * <pre>
 * app.seeding.categories.max-depth=10  # Maximum hierarchy depth
 * </pre>
 *
 * <p>
 * <b>Validation Rules:</b>
 * <ul>
 * <li>Name Pattern: {@code [a-zA-Z0-9\s&',.-]+} (alphanumeric + common
 * punctuation)</li>
 * <li>Name Length: 2-100 characters</li>
 * <li>Max Depth: Configurable (default: 10 levels)</li>
 * </ul>
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class CategoryValidator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}0-9\\s&',.-]+$");
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 100;

    @Value("${app.seeding.categories.max-depth:10}")
    private int maxDepth;

    /**
     * Validates a category node and all its descendants.
     * 
     * <p>
     * Recursively traverses the tree starting from the given node,
     * validating each node against all rules.
     *
     * @param node the root node to validate
     * @throws InvalidCategoryHierarchyException if validation fails
     * @throws NullPointerException              if node is null
     */
    public void validate(CategoryNode node) {
        if (node == null) {
            throw new NullPointerException("CategoryNode cannot be null");
        }

        log.debug("Validating category node: {}", node.name());
        validateRecursive(node, 0);
    }

    /**
     * Recursively validates a node and its children.
     *
     * @param node  the node to validate
     * @param depth the current depth in the hierarchy (0-indexed)
     * @throws InvalidCategoryHierarchyException if validation fails
     */
    private void validateRecursive(CategoryNode node, int depth) {
        // Validate depth
        if (depth > maxDepth) {
            throw InvalidCategoryHierarchyException.maxDepthExceeded(
                    node.name(), depth, maxDepth);
        }

        // Validate name is not blank
        if (node.name() == null || node.name().isBlank()) {
            throw new InvalidCategoryHierarchyException(
                    "Category name cannot be null or blank");
        }

        // Validate name length
        if (node.name().length() < MIN_NAME_LENGTH) {
            throw new InvalidCategoryHierarchyException(
                    String.format("Category name '%s' is too short. Minimum length: %d characters",
                            node.name(), MIN_NAME_LENGTH));
        }

        if (node.name().length() > MAX_NAME_LENGTH) {
            throw InvalidCategoryHierarchyException.nameTooLong(node.name(), MAX_NAME_LENGTH);
        }

        // Validate name pattern
        if (!NAME_PATTERN.matcher(node.name()).matches()) {
            throw InvalidCategoryHierarchyException.invalidName(
                    node.name(), NAME_PATTERN.pattern());
        }

        // Recursively validate children
        for (CategoryNode child : node.children()) {
            validateRecursive(child, depth + 1);
        }
    }

    /**
     * Validates just the name without recursion (for single-node validation).
     *
     * @param name the category name to validate
     * @throws InvalidCategoryHierarchyException if name is invalid
     */
    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidCategoryHierarchyException("Category name cannot be null or blank");
        }

        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            throw InvalidCategoryHierarchyException.nameTooLong(name, MAX_NAME_LENGTH);
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            throw InvalidCategoryHierarchyException.invalidName(name, NAME_PATTERN.pattern());
        }
    }

    /**
     * Gets the configured maximum depth.
     *
     * @return the maximum allowed hierarchy depth
     */
    public int getMaxDepth() {
        return maxDepth;
    }
}
