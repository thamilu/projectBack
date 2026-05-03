package com.eshop.app.exception;

/**
 * Exception thrown when the category hierarchy structure is invalid.
 * 
 * <p>This exception is thrown when:
 * <ul>
 *   <li>Category tree exceeds maximum depth (default: 10 levels)</li>
 *   <li>Circular references are detected in the hierarchy</li>
 *   <li>Category name contains invalid characters</li>
 *   <li>Category name exceeds maximum length (100 characters)</li>
 *   <li>Category name is blank or null</li>
 * </ul>
 *
 * <p><b>Example - Max Depth Violation:</b>
 * <pre>
 * Level 0: Electronics
 * Level 1:   └── Computers
 * Level 2:       └── Laptops
 * ...
 * Level 10:          └── Ultra-thin    &lt;-- OK
 * Level 11:              └── Business  &lt;-- INVALID! Throws this exception
 * </pre>
 *
 * <p>Error Code: SEED_CAT_003
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
public class InvalidCategoryHierarchyException extends CategorySeedingException {

    private static final String ERROR_CODE = "SEED_CAT_003";

    /**
     * Constructs a new InvalidCategoryHierarchyException with the specified detail message.
     *
     * @param message the detail message explaining the hierarchy violation
     */
    public InvalidCategoryHierarchyException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidCategoryHierarchyException for max depth violations.
     *
     * @param categoryName the category that caused the violation
     * @param currentDepth the depth at which the violation occurred
     * @param maxDepth the maximum allowed depth
     * @return a new InvalidCategoryHierarchyException with a formatted message
     */
    public static InvalidCategoryHierarchyException maxDepthExceeded(
            String categoryName, int currentDepth, int maxDepth) {
        return new InvalidCategoryHierarchyException(
            String.format("Category '%s' at depth %d exceeds maximum allowed depth of %d levels", 
                         categoryName, currentDepth, maxDepth));
    }

    /**
     * Constructs a new InvalidCategoryHierarchyException for invalid name patterns.
     *
     * @param categoryName the invalid category name
     * @param pattern the expected pattern
     * @return a new InvalidCategoryHierarchyException with a formatted message
     */
    public static InvalidCategoryHierarchyException invalidName(String categoryName, String pattern) {
        return new InvalidCategoryHierarchyException(
            String.format("Category name '%s' contains invalid characters. Must match pattern: %s", 
                         categoryName, pattern));
    }

    /**
     * Constructs a new InvalidCategoryHierarchyException for name length violations.
     *
     * @param categoryName the too-long category name
     * @param maxLength the maximum allowed length
     * @return a new InvalidCategoryHierarchyException with a formatted message
     */
    public static InvalidCategoryHierarchyException nameTooLong(String categoryName, int maxLength) {
        return new InvalidCategoryHierarchyException(
            String.format("Category name '%s' exceeds maximum length of %d characters (actual: %d)", 
                         categoryName, maxLength, categoryName.length()));
    }

    @Override
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
