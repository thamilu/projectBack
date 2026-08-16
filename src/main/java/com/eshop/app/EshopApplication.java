package com.eshop.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Spring Boot Application Entry Point.
 *
 * <p><b>Enterprise Features Enabled:</b>
 * <ul>
 *   <li>{@link EnableCaching} — Multi-layer caching with Caffeine (L1) and Redis (L2)</li>
 *   <li>{@link EnableRetry} — Retry mechanism for transient failures (e.g. Keycloak, external APIs)</li>
 *   <li>{@link EnableAsync} — Async processing for event-driven architecture</li>
 *   <li>{@link ConfigurationPropertiesScan} — Package-scanned binding for {@code @ConfigurationProperties} classes</li>
 * </ul>
 *
 * <p><b>Repository Configuration:</b> JPA and Redis repository scanning is delegated to dedicated
 * {@code @Configuration} classes to avoid bootstrap ordering conflicts:
 * <ul>
 *   <li>{@link com.eshop.app.config.JpaRepositoryConfig} — JPA repositories</li>
 *   <li>{@link com.eshop.app.config.RedisRepositoryConfig} — Redis repositories (conditional)</li>
 * </ul>
 *
 * @see com.eshop.app.config.JpaRepositoryConfig
 * @see com.eshop.app.config.RedisRepositoryConfig
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableCaching
@EnableRetry
@EnableAsync
public class EshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(EshopApplication.class, args);
    }
}