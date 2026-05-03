package com.eshop.app.seed.provider;

import com.eshop.app.seed.model.CategoryNode;

import java.util.List;

/**
 * Strategy interface for providing category hierarchy data to the seeding process.
 * 
 * <p>This interface allows for multiple implementations to load category data
 * from different sources:
 * <ul>
 *   <li>Code-based: Hardcoded in Java (default implementation)</li>
 *   <li>Properties-based: Loaded from application.properties or YAML</li>
 *   <li>Database-based: Loaded from a configuration table</li>
 *   <li>External API: Fetched from a remote service</li>
 * </ul>
 *
 * <p><b>Example Implementation:</b>
 * <pre>
 * {@code @Component}
 * {@code @Profile("dev")}
 * public class CodeBasedCategoryDataProvider implements CategoryDataProvider {
 *     {@code @Override}
 *     public List&lt;CategoryNode&gt; getCategoryHierarchy() {
 *         return List.of(
 *             CategoryNode.of("Electronics", 
 *                 CategoryNode.leaf("Mobiles"),
 *                 CategoryNode.leaf("Computers")
 *             )
 *         );
 *    }
 *     
 *     {@code @Override}
 *     public String getProviderName() {
 *         return "code-based";
 *     }
 * }
 * </pre>
 *
 * @author E-Shop Team
 * @since 1.0.0
 */
public interface CategoryDataProvider {

    /**
     * Retrieves the complete category hierarchy as a list of root nodes.
     * 
     * <p>Each root node may contain nested children forming the complete category tree.
     *
     * @return a list of CategoryNode objects representing the root categories
     * @throws com.eshop.app.exception.CategorySeedingException if data cannot be loaded
     */
    List<CategoryNode> getCategoryHierarchy();

    /**
     * Gets a human-readable name for this data provider.
     * 
     * <p>Used for logging and debugging purposes to identify the data source.
     *
     * @return the provider name (e.g., "code-based", "properties", "database-config")
     */
    String getProviderName();
}
