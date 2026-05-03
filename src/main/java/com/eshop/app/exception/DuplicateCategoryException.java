package com.eshop.app.exception;

/**
 * Exception thrown when attempting to create a duplicate category within the same hierarchy path.
 * 
 * <p>This exception indicates a violation of the hierarchical uniqueness constraint where
 * the same category name exists under the same parent category.
 *
 * <p><b>Example:</b>
 * <pre>
 * Fashion & Apparel
 *   ├── Men
 *   │   └── T-Shirts  &lt;-- First occurrence
 *   └── Men
 *       └── T-Shirts  &lt;-- Duplicate! Throws this exception
 * </pre>
 *
 * <p>Error Code: SEED_CAT_002
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
public class DuplicateCategoryException extends CategorySeedingException {

    private static final String ERROR_CODE = "SEED_CAT_002";
    
    private final String categoryName;
    private final String parentPath;

    /**
     * Constructs a new DuplicateCategoryException with category and parent context.
     *
     * @param categoryName the name of the duplicate category
     * @param parentPath the full path to the parent category
     */
    public DuplicateCategoryException(String categoryName, String parentPath) {
        super(String.format("Duplicate category '%s' detected under parent path '%s'. " +
                          "Category names must be unique within the same parent.", 
                          categoryName, parentPath != null ? parentPath : "ROOT"));
        this.categoryName = categoryName;
        this.parentPath = parentPath;
    }

    /**
     * Gets the name of the duplicate category.
     *
     * @return the category name
     */
    public String getCategoryName() {
        return categoryName;
    }

    /**
     * Gets the full path to the parent category.
     *
     * @return the parent path, or null if the category is at root level
     */
    public String getParentPath() {
        return parentPath;
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
