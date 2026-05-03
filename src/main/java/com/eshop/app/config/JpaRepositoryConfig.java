package com.eshop.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * JPA Repository Configuration
 * 
 * <p><b>CRITICAL-001 FIX:</b> Explicit separation between JPA and Redis repositories
 * to prevent Spring Data from attempting to scan all repositories as potential Redis repositories.
 * 
 * <h2>Problem:</h2>
 * <pre>
 * Spring Data Redis - Could not safely identify store assignment for repository candidate 
 * interface com.eshop.app.repository.CartRepository
 * [... repeats for 40+ repositories]
 * Finished Spring Data repository scanning in 74 ms. Found 0 Redis repository interfaces.
 * </pre>
 * 
 * <h2>Root Cause:</h2>
 * <ul>
 *   <li>Single @EnableJpaRepositories on main application class scans ALL repositories</li>
 *   <li>Spring Data attempts to determine if each repository is JPA, Redis, MongoDB, etc.</li>
 *   <li>Without explicit filtering, 40+ repositories are scanned unnecessarily</li>
 * </ul>
 * 
 * <h2>Solution:</h2>
 * <ul>
 *   <li>Dedicated JPA configuration with explicit package scanning</li>
 *   <li>Exclude any entities annotated with @RedisHash from JPA scanning</li>
 *   <li>Separate Redis repository configuration (conditional on Redis being enabled)</li>
 * </ul>
 * 
 * <h2>Package Structure:</h2>
 * <pre>
 * com.eshop.app.repository/
 * ├── All JPA repositories (Product, User, Order, etc.)
 * └── (Future: redis/ subfolder if Redis repositories are needed)
 * </pre>
 * 
 * @author EShop Engineering Team
 * @version 1.0
 * @since 2025-12-22
 * @see RedisRepositoryConfig
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.eshop.app.repository",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ANNOTATION,
        classes = RedisHash.class
    )
)
public class JpaRepositoryConfig {
    // Configuration marker - no additional code needed
    // JPA repositories are auto-detected and configured by Spring Data JPA
}
