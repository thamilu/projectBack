package com.eshop.app.exception;

/**
 * Base exception for all category seeding operations.
 * 
 * <p>This exception is thrown when category seeding fails due to:
 * <ul>
 *   <li>Database connectivity issues</li>
 *   <li>Data validation failures</li>
 *   <li>Hierarchy construction errors</li>
 *   <li>Persistence failures</li>
 * </ul>
 *
 * <p>Error Code: SEED_CAT_001
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
public class CategorySeedingException extends RuntimeException {

    private static final String ERROR_CODE = "SEED_CAT_001";
    
    /**
     * Constructs a new CategorySeedingException with the specified detail message.
     *
     * @param message the detail message explaining the seeding failure
     */
    public CategorySeedingException(String message) {
        super(message);
    }

    /**
     * Constructs a new CategorySeedingException with the specified detail message and cause.
     *
     * @param message the detail message explaining the seeding failure
     * @param cause the underlying cause of the failure
     */
    public CategorySeedingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code "SEED_CAT_001"
     */
    public String getErrorCode() {
        return ERROR_CODE;
    }
}
