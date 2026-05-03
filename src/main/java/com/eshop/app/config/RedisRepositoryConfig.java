package com.eshop.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis Repository Configuration
 * 
 * <p><b>CRITICAL-001 FIX:</b> Conditional Redis repository configuration that only activates
 * when Redis is explicitly enabled in application properties.
 * 
 * <h2>Conditional Activation:</h2>
 * <pre>
 * # Enable Redis repositories (default: false)
 * spring.data.redis.repositories.enabled=true
 * </pre>
 * 
 * <h2>Benefits:</h2>
 * <ul>
 *   <li>No repository scanning conflicts when Redis is disabled</li>
 *   <li>Clean separation of concerns between JPA and Redis repositories</li>
 *   <li>Easy to enable Redis repositories when needed in future</li>
 * </ul>
 * 
 * <h2>Package Structure (Future):</h2>
 * <pre>
 * com.eshop.app.repository.redis/
 * ├── SessionCacheRepository.java
 * ├── TokenBlacklistRepository.java
 * └── ... (Redis-specific repositories)
 * </pre>
 * 
 * @author EShop Engineering Team
 * @version 1.0
 * @since 2025-12-22
 * @see JpaRepositoryConfig
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.data.redis.repositories.enabled",
    havingValue = "true",
    matchIfMissing = false
)
@EnableRedisRepositories(
    basePackages = "com.eshop.app.repository.redis"
)
public class RedisRepositoryConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisRepositoryConfig.class);

    /**
     * Log activation after application is ready to avoid constructor-time logging and
     * to ensure the Spring context has finished wiring related beans.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("[REDIS] Repository scanning enabled for package: com.eshop.app.repository.redis");
    }
}
